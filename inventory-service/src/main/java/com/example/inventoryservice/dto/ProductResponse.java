package com.example.inventoryservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        Integer quantity,
        BigDecimal price,
        BigDecimal sale
) {
}
