package com.example.orderservice.service;

import com.example.orderservice.dto.JwtRequest;
import com.example.orderservice.dto.JwtResponse;
import com.example.orderservice.dto.RefreshTokenRequest;

public interface AuthService {

    JwtResponse login(JwtRequest request);

    JwtResponse getAccessToken(RefreshTokenRequest refreshTokenRequest);
}
