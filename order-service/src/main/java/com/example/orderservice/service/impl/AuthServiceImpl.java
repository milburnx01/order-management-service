package com.example.orderservice.service.impl;

import com.example.orderservice.dto.JwtRequest;
import com.example.orderservice.dto.JwtResponse;
import com.example.orderservice.dto.RefreshTokenRequest;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.AuthException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.security.JwtProvider;
import com.example.orderservice.service.AuthService;
import com.example.orderservice.service.UserService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final PasswordEncoder passwordEncoder;

    @Override
    public JwtResponse login(JwtRequest request) {
        try {
            User user = userService.findUserByUsername(request.username());

            if (!passwordEncoder.matches(request.password(), user.getPassword())) {
                throw new AuthException("Invalid username or password");
            }

            String accessToken = jwtProvider.generateAccessToken(user);
            String refreshToken = jwtProvider.generateRefreshToken(user);

            return new JwtResponse(accessToken, refreshToken);

        } catch (UserNotFoundException e) {
            throw new AuthException("Invalid username or password");
        }
    }

    @Override
    public JwtResponse getAccessToken(RefreshTokenRequest refreshTokenRequest) {
        String refreshToken = refreshTokenRequest.refreshToken();
        if (refreshToken != null && jwtProvider.validateRefreshToken(refreshToken)) {
            Claims claims = jwtProvider.getRefreshClaims(refreshToken);
            String username = claims.getSubject();
            User user = userService.findUserByUsername(username);
            String accessToken = jwtProvider.generateAccessToken(user);
            return new JwtResponse(accessToken, refreshToken);
        } else {
            throw new AuthException("Invalid refresh token");
        }
    }
}
