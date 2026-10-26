package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.dto.JwtRequest;
import com.example.orderservice.dto.JwtResponse;
import com.example.orderservice.dto.RefreshTokenRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Авторизация пользователей", description = "Регистрация, авторизация, обновление токена доступа")
public interface AuthApi {

    @Operation(summary = "Регистрация пользователя", description = "Позволяет зарегистрировать пользователя")
    ResponseEntity<JwtResponse> registerUser(CreateUserRequest createUserRequest);

    @Operation(summary = "Авторизация пользователя", description = "Позволяет авторизовать пользователя в системе и получить токен доступа")
    ResponseEntity<JwtResponse> login(JwtRequest jwtRequest);

    @Operation(summary = "Обновление токена доступа", description = "Позволяет обновить токен доступа по refresh токену")
    ResponseEntity<JwtResponse> refreshToken(RefreshTokenRequest refreshTokenRequest);
}
