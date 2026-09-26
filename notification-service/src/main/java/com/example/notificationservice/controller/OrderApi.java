package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

public interface OrderApi {

    Page<OrderResponse> findAll(@RequestParam int page, @RequestParam int size);

    Page<OrderResponse> findAllByOrderId(@RequestParam("orderId") UUID orderId, @RequestParam int page, @RequestParam int size);

    Page<OrderResponse> findAllByUserId(@RequestParam("userId") UUID userId, @RequestParam int page, @RequestParam int size);
}
