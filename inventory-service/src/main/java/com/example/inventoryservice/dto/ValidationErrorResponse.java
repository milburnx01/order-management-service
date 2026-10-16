package com.example.inventoryservice.dto;

import java.util.List;

public record ValidationErrorResponse(List<Violation> violations) {
}
