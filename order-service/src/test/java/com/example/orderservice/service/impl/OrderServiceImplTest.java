package com.example.orderservice.service.impl;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.OrderException;
import com.example.orderservice.exception.OrderPendingException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.grpc.ProductAvailability;
import com.example.orderservice.service.OrderPersistenceService;
import com.example.orderservice.service.UserService;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private UserService userService;

    @Mock
    private OrderPersistenceService orderPersistenceService;

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

        ProductAvailability availability = availableProduct(
                productId, "Laptop", 1000.0, 0.0
        );

        authenticate("john");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        when(userService.findUserByUsername("john"))
                .thenReturn(createUser(userId));

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
        assertThat(result.items().getFirst().quantity())
                .isEqualTo(2);
        assertThat(result.items().getFirst().totalPrice())
                .isEqualByComparingTo("2000.00");

        verify(inventoryClient).checkAvailability(request);
        verify(userService).findUserByUsername("john");

        verify(orderPersistenceService).savePendingOrder(result);

        verify(inventoryClient).writeOffProducts(
                request.items(), result.orderId()
        );

        verify(orderPersistenceService).markOrderCreated(
                result.orderId(), result
        );
    }

    @Test
    void createOrder_ShouldCalculateTotalPrice_WithDiscount() {
        // given
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 3))
        );

        ProductAvailability availability = availableProduct(
                productId, "Phone", 1000.0, 15.0
        );

        authenticate("john");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(availability));

        when(userService.findUserByUsername("john"))
                .thenReturn(createUser(userId));

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

        verify(orderPersistenceService).savePendingOrder(result);

        verify(inventoryClient).writeOffProducts(
                request.items(), result.orderId()
        );

        verify(orderPersistenceService).markOrderCreated(
                result.orderId(), result
        );
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

        ProductAvailability firstAvailability = availableProduct(
                firstProductId, "Laptop", 1000.0, 10.0
        );

        ProductAvailability secondAvailability = availableProduct(
                secondProductId, "Mouse", 100.0, 0.0
        );

        authenticate("john");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(firstAvailability, secondAvailability));

        when(userService.findUserByUsername("john"))
                .thenReturn(createUser(userId));

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

        verify(orderPersistenceService).savePendingOrder(result);

        verify(inventoryClient).writeOffProducts(
                request.items(), result.orderId()
        );

        verify(orderPersistenceService).markOrderCreated(
                result.orderId(), result
        );
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
                .writeOffProducts(anyList(), any(UUID.class));

        verifyNoInteractions(orderPersistenceService);
        verifyNoInteractions(userService);
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
                .writeOffProducts(anyList(), any(UUID.class));

        verifyNoInteractions(orderPersistenceService);
    }

    @Test
    void createOrder_ShouldMarkFailed_WhenWriteOffIsRejected() {
        // given
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        authenticate("john");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        when(userService.findUserByUsername("john"))
                .thenReturn(createUser(userId));

        doThrow(Status.FAILED_PRECONDITION.asRuntimeException())
                .when(inventoryClient)
                .writeOffProducts(eq(request.items()), any(UUID.class));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(StatusRuntimeException.class)
                .satisfies(exception -> {
                    StatusRuntimeException grpcException =
                            (StatusRuntimeException) exception;

                    assertThat(grpcException.getStatus().getCode())
                            .isEqualTo(Status.Code.FAILED_PRECONDITION);
                });

        ArgumentCaptor<OrderResponse> orderCaptor =
                ArgumentCaptor.forClass(OrderResponse.class);

        verify(orderPersistenceService)
                .savePendingOrder(orderCaptor.capture());

        UUID orderId = orderCaptor.getValue().orderId();

        verify(orderPersistenceService).markOrderFailed(orderId);

        verify(orderPersistenceService, never())
                .markOrderCreated(any(UUID.class), any(OrderResponse.class));
    }

    @Test
    void createOrder_ShouldRemainPending_WhenInventoryUnavailable() {
        // given
        UUID productId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        authenticate("john");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        when(userService.findUserByUsername("john"))
                .thenReturn(createUser(userId));

        doThrow(Status.UNAVAILABLE.asRuntimeException())
                .when(inventoryClient)
                .writeOffProducts(eq(request.items()), any(UUID.class));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(OrderPendingException.class)
                .hasCauseInstanceOf(StatusRuntimeException.class);

        verify(orderPersistenceService).savePendingOrder(
                any(OrderResponse.class)
        );

        verify(orderPersistenceService, never())
                .markOrderFailed(any(UUID.class));

        verify(orderPersistenceService, never())
                .markOrderCreated(any(UUID.class), any(OrderResponse.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenUserNotFound() {
        // given
        UUID productId = UUID.randomUUID();

        CreateOrderRequest request = new CreateOrderRequest(
                List.of(new ProductItem(productId.toString(), 2))
        );

        authenticate("unknown");

        when(inventoryClient.checkAvailability(request))
                .thenReturn(List.of(
                        availableProduct(productId, "Laptop", 1000.0, 0.0)
                ));

        when(userService.findUserByUsername("unknown"))
                .thenThrow(new UserNotFoundException(
                        "User with username unknown not found"
                ));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with username unknown not found");

        verify(userService).findUserByUsername("unknown");

        verify(inventoryClient, never())
                .writeOffProducts(anyList(), any(UUID.class));

        verifyNoInteractions(orderPersistenceService);
    }

    private void authenticate(String username) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        username, null, List.of()
                )
        );
    }

    private User createUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setUsername("john");
        return user;
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
                .setAvailableQuantity(10)
                .setIsAvailable(true)
                .build();
    }
}
