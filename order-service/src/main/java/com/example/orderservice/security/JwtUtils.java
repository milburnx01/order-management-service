package com.example.orderservice.security;

import com.example.orderservice.entity.util.Role;
import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtils {

    public static JwtAuthentication generate(Claims claims) {
        JwtAuthentication jwtAuthentication = new JwtAuthentication();
        jwtAuthentication.setRole(claims.get("role", Role.class));
        jwtAuthentication.setUsername(claims.getSubject());
        return jwtAuthentication;
    }

}
