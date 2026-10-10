package com.example.orderservice.controller.impl;

import com.example.orderservice.controller.AuthApi;
import com.example.orderservice.dto.*;
import com.example.orderservice.service.AuthService;
import com.example.orderservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/register")
    @Override
    public ResponseEntity<JwtResponse> registerUser(CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        JwtResponse response = authService.login(new JwtRequest(createUserRequest.username(), createUserRequest.password()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<JwtResponse> login(JwtRequest jwtRequest) {
        return ResponseEntity.ok(authService.login(jwtRequest));
    }

    @GetMapping("/refresh")
    @Override
    public ResponseEntity<JwtResponse> refreshToken(RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.getAccessToken(refreshTokenRequest));
    }
}