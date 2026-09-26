package com.example.notificationservice.service.impl;

import com.example.notificationservice.dto.OrderResponse;
import com.example.notificationservice.mapper.OrderMapper;
import com.example.notificationservice.repository.OrderRepository;
import com.example.notificationservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    @Override
    public Page<OrderResponse> findAll(Pageable pageable) {
        return orderRepository.findAll(pageable).map(orderMapper::toOrderResponse);
    }

    @Override
    public Page<OrderResponse> findAllByOrderId(UUID orderId, Pageable pageable) {
        return orderRepository.findAllByOrderId(orderId, pageable).map(orderMapper::toOrderResponse);
    }

    @Override
    public Page<OrderResponse> findAllByUserId(UUID userId, Pageable pageable) {
        return orderRepository.findAllByUserId(userId, pageable).map(orderMapper::toOrderResponse);
    }
}
