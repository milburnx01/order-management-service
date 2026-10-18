package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Заказы", description = "Создание заказа")
public interface OrderApi {

    @Operation(summary = "Создание заказа")
    OrderResponse createOrder(CreateOrderRequest createOrderRequest);
}
