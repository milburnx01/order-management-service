package com.example.inventoryservice.dto;

import java.math.BigDecimal;

public record CreateProductRequest(
        String name,
        Integer quantity,
        BigDecimal price,
        BigDecimal sale
) {
}
