package com.example.notificationservice.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        List<OrderItem> items,
        BigDecimal totalPrice
) {
}
