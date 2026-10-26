package com.example.inventoryservice.exception.handler;

import com.example.inventoryservice.exception.ProductNotFoundException;
import io.grpc.Status;
import org.springframework.grpc.server.advice.GrpcAdvice;

@GrpcAdvice
public class GrpcExceptionHandler {

    @org.springframework.grpc.server.advice.GrpcExceptionHandler(ProductNotFoundException.class)
    public Status handleProductNotFoundException(ProductNotFoundException exception) {
        return Status.NOT_FOUND.withDescription(exception.getMessage()).withCause(exception.getCause());
    }

}
