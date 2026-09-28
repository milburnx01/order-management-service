package com.example.orderservice.dto;

public record CreateUserRequest(
        String email,
        String username,
        String password
) {
}
