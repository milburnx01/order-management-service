package com.example.orderservice.dto;

import com.example.orderservice.entity.util.Role;

public record UserResponse(
        String id,
        String username,
        String email,
        Role role
) {
}
