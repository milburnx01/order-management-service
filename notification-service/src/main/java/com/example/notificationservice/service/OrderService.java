package com.example.notificationservice.service;

import com.example.notificationservice.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface OrderService {

    Page<OrderResponse> findAll(Pageable pageable);

    Page<OrderResponse> findAllByOrderId(UUID orderId, Pageable pageable);

    Page<OrderResponse> findAllByUserId(UUID userId, Pageable pageable);
}
