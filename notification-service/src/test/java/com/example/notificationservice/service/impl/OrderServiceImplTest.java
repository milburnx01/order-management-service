
package com.example.notificationservice.service.impl;

import com.example.notificationservice.dto.OrderCreatedEvent;
import com.example.notificationservice.dto.OrderItem;
import com.example.notificationservice.dto.OrderResponse;
import com.example.notificationservice.entity.Order;
import com.example.notificationservice.mapper.OrderMapper;
import com.example.notificationservice.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void findAll_ShouldReturnPageOfOrders() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        Order order = Order.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .productId(productId)
                .quantity(2)
                .price(new BigDecimal("100.00"))
                .sale(new BigDecimal("10.00"))
                .totalPrice(new BigDecimal("180.00"))
                .build();

        OrderResponse response = new OrderResponse(
                id,
                orderId,
                userId,
                productId,
                2,
                new BigDecimal("100.00"),
                new BigDecimal("10.00"),
                new BigDecimal("180.00")
        );

        when(orderRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        when(orderMapper.toOrderResponse(order))
                .thenReturn(response);

        // when
        Page<OrderResponse> result = orderService.findAll(pageable);

        // then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(orderRepository).findAll(pageable);
        verify(orderMapper).toOrderResponse(order);
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenNoOrdersExist() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<OrderResponse> result = orderService.findAll(pageable);

        // then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(orderRepository).findAll(pageable);
        verify(orderMapper, never()).toOrderResponse(any(Order.class));
    }

    @Test
    void findAllByOrderId_ShouldReturnOrders_WhenOrderExists() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .productId(productId)
                .quantity(3)
                .price(new BigDecimal("200.00"))
                .sale(BigDecimal.ZERO)
                .totalPrice(new BigDecimal("600.00"))
                .build();

        OrderResponse response = new OrderResponse(
                id,
                orderId,
                userId,
                productId,
                3,
                new BigDecimal("200.00"),
                BigDecimal.ZERO,
                new BigDecimal("600.00")
        );

        when(orderRepository.findAllByOrderId(orderId, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        when(orderMapper.toOrderResponse(order))
                .thenReturn(response);

        // when
        Page<OrderResponse> result =
                orderService.findAllByOrderId(orderId, pageable);

        // then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);

        verify(orderRepository).findAllByOrderId(orderId, pageable);
        verify(orderMapper).toOrderResponse(order);
    }

    @Test
    void findAllByOrderId_ShouldReturnEmptyPage_WhenOrderNotFound() {
        // given
        UUID orderId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        when(orderRepository.findAllByOrderId(orderId, pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<OrderResponse> result =
                orderService.findAllByOrderId(orderId, pageable);

        // then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(orderRepository).findAllByOrderId(orderId, pageable);
        verify(orderMapper, never()).toOrderResponse(any(Order.class));
    }

    @Test
    void findAllByUserId_ShouldReturnOrders_WhenUserHasOrders() {
        // given
        UUID id = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        Order order = Order.builder()
                .id(id)
                .orderId(orderId)
                .userId(userId)
                .productId(productId)
                .quantity(1)
                .price(new BigDecimal("500.00"))
                .sale(new BigDecimal("20.00"))
                .totalPrice(new BigDecimal("400.00"))
                .build();

        OrderResponse response = new OrderResponse(
                id,
                orderId,
                userId,
                productId,
                1,
                new BigDecimal("500.00"),
                new BigDecimal("20.00"),
                new BigDecimal("400.00")
        );

        when(orderRepository.findAllByUserId(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(order), pageable, 1));

        when(orderMapper.toOrderResponse(order))
                .thenReturn(response);

        // when
        Page<OrderResponse> result =
                orderService.findAllByUserId(userId, pageable);

        // then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getContent().getFirst().userId()).isEqualTo(userId);

        verify(orderRepository).findAllByUserId(userId, pageable);
        verify(orderMapper).toOrderResponse(order);
    }

    @Test
    void createOrder_ShouldSaveAllOrderItems() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID firstProductId = UUID.randomUUID();
        UUID secondProductId = UUID.randomUUID();

        OrderItem firstItem = new OrderItem(
                firstProductId,
                "Laptop",
                2,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1800.00")
        );

        OrderItem secondItem = new OrderItem(
                secondProductId,
                "Mouse",
                3,
                new BigDecimal("100.00"),
                BigDecimal.ZERO,
                new BigDecimal("300.00")
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                userId,
                List.of(firstItem, secondItem),
                new BigDecimal("2100.00")
        );

        ArgumentCaptor<Order> orderCaptor =
                ArgumentCaptor.forClass(Order.class);

        // when
        orderService.createOrder(event);

        // then
        verify(orderRepository, times(2)).save(orderCaptor.capture());

        List<Order> savedOrders = orderCaptor.getAllValues();

        assertThat(savedOrders).hasSize(2);

        Order firstOrder = savedOrders.get(0);

        assertThat(firstOrder.getOrderId()).isEqualTo(orderId);
        assertThat(firstOrder.getUserId()).isEqualTo(userId);
        assertThat(firstOrder.getProductId()).isEqualTo(firstProductId);
        assertThat(firstOrder.getQuantity()).isEqualTo(2);
        assertThat(firstOrder.getPrice()).isEqualByComparingTo("1000.00");
        assertThat(firstOrder.getSale()).isEqualByComparingTo("10.00");
        assertThat(firstOrder.getTotalPrice())
                .isEqualByComparingTo("1800.00");

        Order secondOrder = savedOrders.get(1);

        assertThat(secondOrder.getOrderId()).isEqualTo(orderId);
        assertThat(secondOrder.getUserId()).isEqualTo(userId);
        assertThat(secondOrder.getProductId()).isEqualTo(secondProductId);
        assertThat(secondOrder.getQuantity()).isEqualTo(3);
        assertThat(secondOrder.getPrice()).isEqualByComparingTo("100.00");
        assertThat(secondOrder.getSale()).isEqualByComparingTo("0.00");
        assertThat(secondOrder.getTotalPrice())
                .isEqualByComparingTo("300.00");
    }

    @Test
    void createOrder_ShouldNotSaveAnything_WhenItemsAreEmpty() {
        // given
        OrderCreatedEvent event = new OrderCreatedEvent(
                UUID.randomUUID(),
                UUID.randomUUID(),
                List.of(),
                BigDecimal.ZERO
        );

        // when
        orderService.createOrder(event);

        // then
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_ShouldThrowException_WhenRepositorySaveFails() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        OrderItem item = new OrderItem(
                productId,
                "Laptop",
                2,
                new BigDecimal("1000.00"),
                new BigDecimal("10.00"),
                new BigDecimal("1800.00")
        );

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId,
                userId,
                List.of(item),
                new BigDecimal("1800.00")
        );

        when(orderRepository.save(any(Order.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "Failed to save order"
                ));

        // when & then
        assertThatThrownBy(() -> orderService.createOrder(event))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessage("Failed to save order");

        verify(orderRepository).save(any(Order.class));
    }
}
