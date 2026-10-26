package com.example.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record ProductItem(
        @NotBlank(message = "Product ID is required")
        String id,
        @Positive(message = "Product quantity must be greater than 0")
        int quantity
) {}
