package com.example.notificationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID orderId,
        UUID userId,
        UUID productId,
        Integer quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal totalPrice
) {
}
