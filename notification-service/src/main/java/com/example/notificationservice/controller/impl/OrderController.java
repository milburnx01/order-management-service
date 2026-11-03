package com.example.notificationservice.controller.impl;

import com.example.notificationservice.controller.OrderApi;
import com.example.notificationservice.dto.OrderResponse;
import com.example.notificationservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController implements OrderApi {

    private final OrderService orderService;

    @GetMapping("/all")
    @Override
    public Page<OrderResponse> findAll(@RequestParam("page") int page, @RequestParam("size") int size) {
        return orderService.findAll(PageRequest.of(page, size));
    }

    @GetMapping("/{orderId}")
    @Override
    public Page<OrderResponse> findAllByOrderId(@PathVariable("orderId") UUID orderId,
                                                @RequestParam("page") int page,
                                                @RequestParam("size") int size) {
        return orderService.findAllByOrderId(orderId, PageRequest.of(page, size));
    }

    @GetMapping("/user/{userId}")
    @Override
    public Page<OrderResponse> findAllByUserId(@PathVariable("userId") UUID userId,
                                               @RequestParam("page") int page,
                                               @RequestParam("size") int size) {
        return orderService.findAllByUserId(userId, PageRequest.of(page, size));
    }
}
