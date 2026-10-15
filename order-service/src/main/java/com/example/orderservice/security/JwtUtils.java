package com.example.orderservice.security;

import com.example.orderservice.entity.util.Role;
import io.jsonwebtoken.Claims;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class JwtUtils {

    public static JwtAuthentication generate(Claims claims) {
        JwtAuthentication jwtAuthentication = new JwtAuthentication();

        String role = claims.get("role", String.class);

        jwtAuthentication.setRole(Role.valueOf(role));
        jwtAuthentication.setUsername(claims.getSubject());
        return jwtAuthentication;
    }

}
