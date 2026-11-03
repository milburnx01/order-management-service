package com.example.orderservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Builder
public record OrderResponse(
        UUID orderId,
        UUID userId,
        List<OrderItemResponse> items,
        BigDecimal totalPrice
) {
}
