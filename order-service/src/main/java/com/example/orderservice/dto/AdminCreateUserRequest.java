package com.example.orderservice.dto;

import com.example.orderservice.entity.util.Role;

public record AdminCreateUserRequest(
        String username,
        String password,
        String email,
        Role role
) {
}
