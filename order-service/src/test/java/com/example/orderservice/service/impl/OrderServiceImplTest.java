
package com.example.orderservice.service.impl;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.OrderException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.grpc.ProductAvailability;
import com.example.orderservice.kafka.OrderKafkaProducer;
import com.example.orderservice.service.UserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private UserService userService;

    @Mock
    private OrderKafkaProducer orderKafkaProducer;

    @InjectMocks
    private OrderServiceImpl orderService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createOrder_ShouldReturnOrder_WhenProductsAreAvailable() {
        // given
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Laptop")
                .setPrice(1000.0)
                .setSale(0.0)
                .setAvailableQuantity(10)
                .setIsAvailable(true)
                .build();

        User user = new User();
        user.setId(userId);
        user.setUsername("john");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "john", null, List.of()
                )
        );

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        // when
        OrderResponse result = orderService.createOrder(request);

        // then
        assertThat(result.orderId()).isNotNull();
        assertThat(result.userId()).isEqualTo(userId);
        assertThat(result.totalPrice())
                .isEqualByComparingTo("2000.00");

        assertThat(result.items()).hasSize(1);
        assertThat(result.items().getFirst().productId())
                .isEqualTo(productId);
        assertThat(result.items().getFirst().productName())
                .isEqualTo("Laptop");
        assertThat(result.items().getFirst().quantity()).isEqualTo(2);
        assertThat(result.items().getFirst().totalPrice())
                .isEqualByComparingTo("2000.00");

        verify(inventoryClient).checkAvailability(request);
        verify(inventoryClient).writeOffProductProducts(request.items());
        verify(userService).findUserByUsername("john");
        verify(orderKafkaProducer).sendOrder(result);
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice_WithDiscount() {
        // given
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 3))
        );

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setName("Phone")
                .setPrice(1000.0)
                .setSale(15.0)
                .setAvailableQuantity(10)
                .setIsAvailable(true)
                .build();

        User user = new User();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "john", null, List.of()
                )
        );

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        // when
        OrderResponse result = orderService.createOrder(request);

        // then
        assertThat(result.totalPrice())
                .isEqualByComparingTo("2550.00");

        assertThat(result.items().getFirst().price())
                .isEqualByComparingTo("1000.00");

        assertThat(result.items().getFirst().sale())
                .isEqualByComparingTo("15.00");

        assertThat(result.items().getFirst().totalPrice())
                .isEqualByComparingTo("2550.00");

        verify(inventoryClient).writeOffProductProducts(request.items());
        verify(orderKafkaProducer).sendOrder(result);
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice_ForMultipleProducts() {
        // given
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new ProductItem(firstProductId.toString(), 2),
                        new ProductItem(secondProductId.toString(), 3)
                )
        );

        ProductAvailability firstAvailability =
                ProductAvailability.newBuilder()
                        .setId(firstProductId.toString())
                        .setName("Laptop")
                        .setPrice(1000.0)
                        .setSale(10.0)
                        .setAvailableQuantity(5)
                        .setIsAvailable(true)
                        .build();

        ProductAvailability secondAvailability =
                ProductAvailability.newBuilder()
                        .setId(secondProductId.toString())
                        .setName("Mouse")
                        .setPrice(100.0)
                        .setSale(0.0)
                        .setAvailableQuantity(20)
                        .setIsAvailable(true)
                        .build();

        User user = new User();
        user.setId(userId);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "john", null, List.of()
                )
        );

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(firstAvailability, secondAvailability));

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        // when
        OrderResponse result = orderService.createOrder(request);

        // then
        assertThat(result.items()).hasSize(2);

        assertThat(result.items().get(0).totalPrice())
                .isEqualByComparingTo("1800.00");

        assertThat(result.items().get(1).totalPrice())
                .isEqualByComparingTo("300.00");

        assertThat(result.totalPrice())
                .isEqualByComparingTo("2100.00");

        verify(inventoryClient).writeOffProductProducts(request.items());
        verify(orderKafkaProducer).sendOrder(result);
    }

    @Test
    void createOrder_ShouldThrowException_WhenProductNotAvailable() {
        // given
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 10))
        );

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setIsAvailable(false)
                .build();

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderException.class)
                .hasMessage("Products not available: " + productId);

        verify(inventoryClient).checkAvailability(request);

        verify(inventoryClient, never())
                .writeOffProductProducts(any());

        verify(userService, never())
                .findUserByUsername(any());

        verify(orderKafkaProducer, never())
                .sendOrder(any());
    }

    @Test
    void createOrder_ShouldThrowException_WhenMultipleProductsNotAvailable() {
        // given
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(
                        new ProductItem(firstProductId.toString(), 10),
                        new ProductItem(secondProductId.toString(), 20)
                )
        );

        ProductAvailability firstAvailability =
                ProductAvailability.newBuilder()
                        .setId(firstProductId.toString())
                        .setIsAvailable(false)
                        .build();

        ProductAvailability secondAvailability =
                ProductAvailability.newBuilder()
                        .setId(secondProductId.toString())
                        .setIsAvailable(false)
                        .build();

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(firstAvailability, secondAvailability));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderException.class)
                .hasMessage(
                        "Products not available: "
                                + firstProductId + ", " + secondProductId
                );

        verify(inventoryClient, never())
                .writeOffProductProducts(any());

        verify(orderKafkaProducer, never())
                .sendOrder(any());
    }

    @Test
    void createOrder_ShouldNotSendEvent_WhenWriteOffFails() {
        // given
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setIsAvailable(true)
                .build();

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        doThrow(new OrderException("Write-off failed"))
                .when(inventoryClient)
                .writeOffProductProducts(request.items());

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderException.class)
                .hasMessage("Write-off failed");

        verify(inventoryClient).checkAvailability(request);
        verify(inventoryClient).writeOffProductProducts(request.items());

        verify(userService, never())
                .findUserByUsername(any());

        verify(orderKafkaProducer, never())
                .sendOrder(any());
    }

    @Test
    void createOrder_ShouldNotSendEvent_WhenUserNotFound() {
        // given
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        ProductAvailability availability = ProductAvailability.newBuilder()
                .setId(productId.toString())
                .setIsAvailable(true)
                .build();

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "unknown", null, List.of()
                )
        );

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        when(userService.findUserByUsername("unknown"))
                .thenThrow(new UserNotFoundException(
                        "User with username unknown not found"
                ));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with username unknown not found");

        verify(inventoryClient).writeOffProductProducts(request.items());
        verify(userService).findUserByUsername("unknown");

        verify(orderKafkaProducer, never())
                .sendOrder(any());
    }
}
