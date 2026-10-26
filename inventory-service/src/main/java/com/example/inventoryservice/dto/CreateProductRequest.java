package com.example.inventoryservice.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateProductRequest(

        @NotBlank(message = "Product name is required")
        @Size(max = 255, message = "Product name must not exceed 255 characters")
        String name,

        @NotNull(message = "Product quantity is required")
        @Positive(message = "Product quantity must be greater than 0")
        Integer quantity,

        @NotNull(message = "Product price is required")
        @PositiveOrZero(message = "Product price must be greater than or equal to 0")
        @Digits(integer = 17, fraction = 2,
                message = "Product price must have at most 17 integer digits and 2 decimal places")
        BigDecimal price,

        @NotNull(message = "Product sale is required")
        @DecimalMin(value = "0.00", message = "Product sale must be at least 0%")
        @DecimalMax(value = "100.0", message = "Product sale must not exceed 100%")
        BigDecimal sale

) {}
