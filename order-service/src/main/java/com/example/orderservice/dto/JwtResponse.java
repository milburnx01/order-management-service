package com.example.orderservice.dto;

public record JwtResponse(
        String accessToken,
        String refreshToken
) {
}
