package com.example.orderservice.controller.impl;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.User;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.exception.OrderException;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.grpc.ProductAvailability;
import com.example.orderservice.kafka.OrderKafkaProducer;
import com.example.orderservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(username = "user", authorities = "USER")
class OrderControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    @MockitoBean
    private OrderKafkaProducer orderKafkaProducer;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createOrder_ShouldReturnOrder_WhenProductIsAvailable() throws Exception {
        // given
        User user = userRepository.saveAndFlush(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));

        UUID productId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(productId);

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Laptop")
                .setPrice(1000.0)
                .setSale(0.0)
                .setIsAvailable(true)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.userId").value(user.getId().toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName").value("Laptop"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice").value(2000.0))
                .andExpect(jsonPath("$.totalPrice").value(2000.0));

        // then
        verify(inventoryClient).writeOffProductProducts(
                List.of(new ProductItem(productId.toString(), 2))
        );

        ArgumentCaptor<OrderResponse> captor =
                ArgumentCaptor.forClass(OrderResponse.class);

        verify(orderKafkaProducer).sendOrder(captor.capture());

        OrderResponse sentOrder = captor.getValue();

        assertThat(sentOrder.userId()).isEqualTo(user.getId());
        assertThat(sentOrder.items()).hasSize(1);
        assertThat(sentOrder.totalPrice())
                .isEqualByComparingTo(new BigDecimal("2000.00"));
    }

    @Test
    void createOrder_ShouldApplyDiscount_WhenProductHasSale() throws Exception {
        // given
        userRepository.saveAndFlush(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));

        UUID productId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 3
                        }
                    ]
                }
                """.formatted(productId);

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Keyboard")
                .setPrice(200.0)
                .setSale(10.0)
                .setIsAvailable(true)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price").value(200.0))
                .andExpect(jsonPath("$.items[0].sale").value(10.0))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].totalPrice").value(540.0))
                .andExpect(jsonPath("$.totalPrice").value(540.0));

        // then
        ArgumentCaptor<OrderResponse> captor =
                ArgumentCaptor.forClass(OrderResponse.class);

        verify(orderKafkaProducer).sendOrder(captor.capture());

        assertThat(captor.getValue().totalPrice())
                .isEqualByComparingTo(new BigDecimal("540.00"));
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice_WhenOrderContainsMultipleProducts() throws Exception {
        // given
        userRepository.saveAndFlush(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));

        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 2
                        },
                        {
                            "id": "%s",
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(firstProductId, secondProductId);

        ProductAvailability firstAvailability = ProductAvailability.newBuilder()
                .setId(firstProductId.toString())
                .setName("Mouse")
                .setPrice(100.0)
                .setSale(0.0)
                .setIsAvailable(true)
                .build();

        ProductAvailability secondAvailability = ProductAvailability.newBuilder()
                .setId(secondProductId.toString())
                .setName("Monitor")
                .setPrice(500.0)
                .setSale(20.0)
                .setIsAvailable(true)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(firstAvailability, secondAvailability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice").value(200.0))
                .andExpect(jsonPath("$.items[1].totalPrice").value(400.0))
                .andExpect(jsonPath("$.totalPrice").value(600.0));

        // then
        verify(inventoryClient).writeOffProductProducts(List.of(
                new ProductItem(firstProductId.toString(), 2),
                new ProductItem(secondProductId.toString(), 1)
        ));

        ArgumentCaptor<OrderResponse> captor =
                ArgumentCaptor.forClass(OrderResponse.class);

        verify(orderKafkaProducer).sendOrder(captor.capture());

        assertThat(captor.getValue().items()).hasSize(2);
        assertThat(captor.getValue().totalPrice())
                .isEqualByComparingTo(new BigDecimal("600.00"));
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenProductIsUnavailable() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 10
                        }
                    ]
                }
                """.formatted(productId);

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Laptop")
                .setIsAvailable(false)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Products not available: " + productId));

        // then
        verify(inventoryClient, never()).writeOffProductProducts(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenWriteOffFails() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 2
                        }
                    ]
                }
                """.formatted(productId);

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Laptop")
                .setPrice(1000.0)
                .setSale(0.0)
                .setIsAvailable(true)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        doThrow(new OrderException("Inventory write-off failed"))
                .when(inventoryClient)
                .writeOffProductProducts(any());

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Inventory write-off failed"));

        // then
        verify(inventoryClient).writeOffProductProducts(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenUserDoesNotExist() throws Exception {
        // given
        UUID productId = UUID.randomUUID();

        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(productId);

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Laptop")
                .setPrice(1000.0)
                .setIsAvailable(true)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with username user not found"));

        // then
        verify(inventoryClient).writeOffProductProducts(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenItemsAreEmpty() throws Exception {
        // given
        String request = """
                {
                    "items": []
                }
                """;

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        // then
        verify(inventoryClient, never()).checkAvailability(any());
        verify(inventoryClient, never()).writeOffProductProducts(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenQuantityIsNegative() throws Exception {
        // given
        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": -1
                        }
                    ]
                }
                """.formatted(UUID.randomUUID());

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        // then
        verify(inventoryClient, never()).checkAvailability(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }

    @Test
    @WithAnonymousUser
    void createOrder_ShouldReturnUnauthorized_WhenUserIsNotAuthenticated() throws Exception {
        // given
        String request = """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": 1
                        }
                    ]
                }
                """.formatted(UUID.randomUUID());

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isForbidden());

        // then
        verify(inventoryClient, never()).checkAvailability(any());
        verify(orderKafkaProducer, never()).sendOrder(any());
    }
}