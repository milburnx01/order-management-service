package com.example.inventoryservice.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank
        @Size(max = 255)
        String name,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 17, fraction = 2)
        BigDecimal price,

        @NotNull
        @DecimalMin("0.00")
        @DecimalMax("100.0")
        BigDecimal sale
) {}
