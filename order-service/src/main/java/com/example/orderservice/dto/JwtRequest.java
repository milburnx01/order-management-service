package com.example.orderservice.dto;

import jakarta.validation.constraints.NotBlank;

public record JwtRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Password is required")
        String password

) {}
