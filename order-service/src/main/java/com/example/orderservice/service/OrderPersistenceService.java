package com.example.orderservice.service;

import com.example.orderservice.dto.OrderResponse;

import java.util.UUID;


public interface OrderPersistenceService {

    void savePendingOrder(OrderResponse response);

    void markOrderCreated(UUID orderId, OrderResponse orderResponse);

    void markOrderFailed(UUID orderId);

}
