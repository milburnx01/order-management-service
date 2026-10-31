
package com.example.orderservice.service.impl;

import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.OrderItem;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.util.OrderStatus;
import com.example.orderservice.entity.util.OutboxStatus;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.repository.OutboxEventRepository;
import com.example.orderservice.service.OrderPersistenceService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderPersistenceServiceImpl implements OrderPersistenceService {

    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public void savePendingOrder(OrderResponse response) {

        Order order = new Order();

        order.setId(response.orderId());
        order.setUserId(response.userId());
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(response.totalPrice());

        for (OrderItemResponse product : response.items()) {
            OrderItem item = new OrderItem();
            item.setProductId(product.productId());
            item.setProductName(product.productName());
            item.setQuantity(product.quantity());
            item.setPrice(product.price());
            item.setSale(product.sale());
            item.setTotalPrice(product.totalPrice());
            order.addItem(item);
        }
        orderRepository.save(order);
    }

    @Override
    @Transactional
    public void markOrderCreated(UUID orderId, OrderResponse response) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        if (!orderId.equals(response.orderId())) {
            throw new IllegalArgumentException("Order ID does not match response");
        }

        if (order.getStatus() == OrderStatus.CREATED) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Invalid order status: " + order.getStatus());
        }

        String payload = objectMapper.writeValueAsString(response);

        OutboxEvent event = new OutboxEvent();
        event.setOrderId(orderId);
        event.setTopic("orders");
        event.setPayload(payload);
        event.setStatus(OutboxStatus.PENDING);

        order.setStatus(OrderStatus.CREATED);

        outboxEventRepository.save(event);
    }

    @Override
    @Transactional
    public void markOrderFailed(UUID orderId) {

        Order order = orderRepository.findById(orderId).orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));

        if (order.getStatus() == OrderStatus.FAILED) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalStateException("Cannot fail order with status: " + order.getStatus());
        }

        order.setStatus(OrderStatus.FAILED);
    }
}
