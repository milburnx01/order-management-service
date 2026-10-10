package com.example.orderservice.dto;

public record JwtRequest(
        String username,
        String password
) {
}
