package com.example.orderservice.controller.impl;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.User;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.grpc.ProductAvailability;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxEventRepository;
import com.example.orderservice.repository.UserRepository;
import io.grpc.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
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

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @MockitoBean
    private InventoryClient inventoryClient;

    @BeforeEach
    void setUp() {
        outboxEventRepository.deleteAll();
        orderRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void createOrder_ShouldReturnOrder_WhenProductIsAvailable()
            throws Exception {

        // given
        User user = createUser();
        UUID productId = UUID.randomUUID();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        // when
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.userId")
                        .value(user.getId().toString()))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].productId")
                        .value(productId.toString()))
                .andExpect(jsonPath("$.items[0].productName")
                        .value("Laptop"))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice").value(2000.0))
                .andExpect(jsonPath("$.totalPrice").value(2000.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        UUID orderId = extractOrderId(response);

        verify(inventoryClient).writeOffProducts(
                eq(List.of(new ProductItem(productId.toString(), 2))),
                eq(orderId)
        );

        assertCreatedOrderAndOutboxEvent(orderId);
    }

    @Test
    void createOrder_ShouldApplyDiscount_WhenProductHasSale()
            throws Exception {

        // given
        createUser();
        UUID productId = UUID.randomUUID();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(productId, "Keyboard", 200.0, 10.0)
                ));

        // when
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 3)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].price").value(200.0))
                .andExpect(jsonPath("$.items[0].sale").value(10.0))
                .andExpect(jsonPath("$.items[0].quantity").value(3))
                .andExpect(jsonPath("$.items[0].totalPrice").value(540.0))
                .andExpect(jsonPath("$.totalPrice").value(540.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        assertCreatedOrderAndOutboxEvent(extractOrderId(response));
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice_WhenOrderContainsMultipleProducts()
            throws Exception {

        // given
        createUser();

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

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(firstProductId, "Mouse", 100.0, 0.0),
                        availableProduct(secondProductId, "Monitor", 500.0, 20.0)
                ));

        // when
        String response = mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].totalPrice").value(200.0))
                .andExpect(jsonPath("$.items[1].totalPrice").value(400.0))
                .andExpect(jsonPath("$.totalPrice").value(600.0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        UUID orderId = extractOrderId(response);

        verify(inventoryClient).writeOffProducts(
                eq(List.of(
                        new ProductItem(firstProductId.toString(), 2),
                        new ProductItem(secondProductId.toString(), 1)
                )),
                eq(orderId)
        );

        assertCreatedOrderAndOutboxEvent(orderId);
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenProductIsUnavailable()
            throws Exception {

        // given
        UUID productId = UUID.randomUUID();

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setIsAvailable(false)
                .build();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(availability));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 10)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Products not available: " + productId));

        // then
        verify(inventoryClient, never())
                .writeOffProducts(anyList(), any(UUID.class));

        assertThat(orderRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void createOrder_ShouldReturnConflict_WhenWriteOffIsRejected()
            throws Exception {

        // given
        createUser();
        UUID productId = UUID.randomUUID();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        doThrow(Status.FAILED_PRECONDITION.asRuntimeException())
                .when(inventoryClient)
                .writeOffProducts(anyList(), any(UUID.class));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 2)))
                .andExpect(status().isConflict());

        // then
        assertThat(orderRepository.count()).isEqualTo(1);

        assertThat(orderRepository.findAll().getFirst().getStatus().name())
                .isEqualTo("FAILED");

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void createOrder_ShouldReturnAccepted_WhenInventoryUnavailable()
            throws Exception {

        // given
        createUser();
        UUID productId = UUID.randomUUID();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        doThrow(Status.UNAVAILABLE.asRuntimeException())
                .when(inventoryClient)
                .writeOffProducts(anyList(), any(UUID.class));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 2)))
                .andExpect(status().isAccepted());

        // then
        assertThat(orderRepository.count()).isEqualTo(1);

        assertThat(orderRepository.findAll().getFirst().getStatus().name())
                .isEqualTo("PENDING");

        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenUserDoesNotExist()
            throws Exception {

        // given
        UUID productId = UUID.randomUUID();

        given(inventoryClient.checkAvailability(any(CreateOrderRequest.class)))
                .willReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        // when
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(productId, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with username user not found"));

        // then
        verify(inventoryClient, never())
                .writeOffProducts(anyList(), any(UUID.class));

        assertThat(orderRepository.count()).isZero();
        assertThat(outboxEventRepository.count()).isZero();
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenItemsAreEmpty()
            throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                    "items": []
                                }
                                """))
                .andExpect(status().isBadRequest());

        verify(inventoryClient, never())
                .checkAvailability(any());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    void createOrder_ShouldReturnBadRequest_WhenQuantityIsNegative()
            throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(UUID.randomUUID(), -1)))
                .andExpect(status().isBadRequest());

        verify(inventoryClient, never())
                .checkAvailability(any());

        assertThat(orderRepository.count()).isZero();
    }

    @Test
    @WithAnonymousUser
    void createOrder_ShouldReturnForbidden_WhenUserIsNotAuthenticated()
            throws Exception {

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson(UUID.randomUUID(), 1)))
                .andExpect(status().isForbidden());

        verify(inventoryClient, never())
                .checkAvailability(any());

        assertThat(orderRepository.count()).isZero();
    }

    private User createUser() {
        return userRepository.saveAndFlush(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));
    }

    private ProductAvailability availableProduct(
            UUID productId,
            String name,
            double price,
            double sale
    ) {
        return ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName(name)
                .setPrice(price)
                .setSale(sale)
                .setIsAvailable(true)
                .build();
    }

    private String requestJson(UUID productId, int quantity) {
        return """
                {
                    "items": [
                        {
                            "id": "%s",
                            "quantity": %d
                        }
                    ]
                }
                """.formatted(productId, quantity);
    }

    private UUID extractOrderId(String response) {
        return UUID.fromString(
                com.jayway.jsonpath.JsonPath.read(
                        response, "$.orderId"
                )
        );
    }

    private void assertCreatedOrderAndOutboxEvent(UUID orderId) {
        var order = orderRepository.findById(orderId).orElseThrow();

        assertThat(order.getStatus().name()).isEqualTo("CREATED");

        assertThat(outboxEventRepository.findAll())
                .anySatisfy(event -> {
                    assertThat(event.getOrderId()).isEqualTo(orderId);
                    assertThat(event.getTopic()).isEqualTo("orders");
                    assertThat(event.getPayload())
                            .contains(orderId.toString());
                });
    }
}
