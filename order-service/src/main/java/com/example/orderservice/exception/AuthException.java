package com.example.orderservice.exception;

public class AuthException extends RuntimeException {
    public AuthException(String message) {
        super(message);
    }
}
