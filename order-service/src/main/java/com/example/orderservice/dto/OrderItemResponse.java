package com.example.orderservice.dto;

import lombok.Builder;

import java.math.BigDecimal;
import java.util.UUID;

@Builder
public record OrderItemResponse(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal totalPrice
) {
}
