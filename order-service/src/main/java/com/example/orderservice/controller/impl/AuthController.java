package com.example.orderservice.controller.impl;

import com.example.orderservice.controller.AuthApi;
import com.example.orderservice.dto.*;
import com.example.orderservice.service.AuthService;
import com.example.orderservice.service.UserService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    private final AuthService authService;
    private final UserService userService;


    @Transactional
    @PostMapping("/register")
    @Override
    public ResponseEntity<JwtResponse> registerUser(@RequestBody CreateUserRequest createUserRequest) {
        userService.createUser(createUserRequest);
        JwtResponse response = authService.login(new JwtRequest(createUserRequest.username(), createUserRequest.password()));
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    @Override
    public ResponseEntity<JwtResponse> login(@RequestBody JwtRequest jwtRequest) {
        return ResponseEntity.ok(authService.login(jwtRequest));
    }

    @GetMapping("/refresh")
    @Override
    public ResponseEntity<JwtResponse> refreshToken(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(authService.getAccessToken(refreshTokenRequest));
    }
}