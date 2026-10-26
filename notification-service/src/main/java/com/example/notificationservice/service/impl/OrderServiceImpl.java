package com.example.notificationservice.service.impl;

import com.example.notificationservice.dto.OrderCreatedEvent;
import com.example.notificationservice.dto.OrderItem;
import com.example.notificationservice.dto.OrderResponse;
import com.example.notificationservice.entity.Order;
import com.example.notificationservice.mapper.OrderMapper;
import com.example.notificationservice.repository.OrderRepository;
import com.example.notificationservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public Page<OrderResponse> findAll(Pageable pageable) {
        log.info("Finding all orders, pageable={}", pageable);
        Page<OrderResponse> response = orderRepository.findAll(pageable).map(orderMapper::toOrderResponse);
        log.info("Orders found successfully, totalElements={}", response.getTotalElements());
        return response;
    }

    @Override
    public Page<OrderResponse> findAllByOrderId(UUID orderId, Pageable pageable) {
        log.info("Finding orders by orderId={}, pageable={}", orderId, pageable);
        Page<OrderResponse> response = orderRepository.findAllByOrderId(orderId, pageable).map(orderMapper::toOrderResponse);
        log.info("Orders found successfully, orderId={}, totalElements={}", orderId, response.getTotalElements());
        return response;
    }

    @Override
    public Page<OrderResponse> findAllByUserId(UUID userId, Pageable pageable) {
        log.info("Finding orders by userId={}, pageable={}",
                userId, pageable);
        Page<OrderResponse> response =orderRepository.findAllByUserId(userId, pageable).map(orderMapper::toOrderResponse);
        log.info("Orders found successfully, userId={}, totalElements={}", userId, response.getTotalElements());
        return response;
    }

    @Transactional
    @Override
    public void createOrder(OrderCreatedEvent orderCreatedEvent) {
        log.info("Creating order, orderId={}, userId={}, itemsCount={}",
                orderCreatedEvent.orderId(),
                orderCreatedEvent.userId(),
                orderCreatedEvent.items().size());

        for (OrderItem item: orderCreatedEvent.items()) {
            Order order = Order.builder()
                    .orderId(orderCreatedEvent.orderId())
                    .userId(orderCreatedEvent.userId())
                    .productId(item.productId())
                    .quantity(item.quantity())
                    .price(item.price())
                    .sale(item.sale())
                    .totalPrice(item.totalPrice())
                    .build();
            orderRepository.save(order);
        }
        log.info("Order processing completed, orderId={}, itemsCount={}",
                orderCreatedEvent.orderId(),
                orderCreatedEvent.items().size());
    }
}
