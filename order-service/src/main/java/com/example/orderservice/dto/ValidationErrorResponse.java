package com.example.orderservice.dto;

import java.util.List;

public record ValidationErrorResponse(List<Violation> violations) {
}
