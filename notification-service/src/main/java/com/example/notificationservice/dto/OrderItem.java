package com.example.notificationservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItem(
        UUID productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal sale,
        BigDecimal totalPrice
) {
}
