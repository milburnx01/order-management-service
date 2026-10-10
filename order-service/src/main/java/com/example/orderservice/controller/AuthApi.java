package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.dto.JwtRequest;
import com.example.orderservice.dto.JwtResponse;
import com.example.orderservice.dto.RefreshTokenRequest;
import org.springframework.http.ResponseEntity;

public interface AuthApi {

    ResponseEntity<JwtResponse> registerUser(CreateUserRequest createUserRequest);

    ResponseEntity<JwtResponse> login(JwtRequest jwtRequest);

    ResponseEntity<JwtResponse> refreshToken(RefreshTokenRequest refreshTokenRequest);
}
