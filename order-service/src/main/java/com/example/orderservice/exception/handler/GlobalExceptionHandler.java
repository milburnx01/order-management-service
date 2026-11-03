package com.example.orderservice.exception.handler;

import com.example.orderservice.dto.ErrorResponse;
import com.example.orderservice.dto.ValidationErrorResponse;
import com.example.orderservice.dto.Violation;
import com.example.orderservice.exception.AuthException;
import com.example.orderservice.exception.OrderException;
import com.example.orderservice.exception.OrderPendingException;
import com.example.orderservice.exception.UserNotFoundException;
import io.grpc.StatusRuntimeException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ErrorResponse> handleAuthException(AuthException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(OrderException.class)
    public ResponseEntity<ErrorResponse> handleOrderException(OrderException e) {
        return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<Violation> violations = e.getBindingResult().getFieldErrors().stream()
                .map(error -> new Violation(error.getField(), error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest().body(new ValidationErrorResponse(violations));
    }

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcException(
            StatusRuntimeException e
    ) {
        HttpStatus status = switch (e.getStatus().getCode()) {
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case FAILED_PRECONDITION -> HttpStatus.CONFLICT;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };

        log.error("Inventory gRPC error: {}", e.getStatus(), e);

        return ResponseEntity.status(status).body(new ErrorResponse("Inventory service error: " + e.getStatus().getCode()));
    }

    @ExceptionHandler(OrderPendingException.class)
    public ResponseEntity<ErrorResponse> handleOrderPending(
            OrderPendingException e
    ) {
        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body(new ErrorResponse(
                        "Order " + e.getOrderId() + " is pending"
                ));
    }
}
