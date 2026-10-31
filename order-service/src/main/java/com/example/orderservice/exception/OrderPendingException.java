package com.example.orderservice.exception;

import lombok.Getter;

import java.util.UUID;

@Getter
public class OrderPendingException extends RuntimeException {

  private final UUID orderId;

  public OrderPendingException(UUID orderId, Throwable cause) {
    super("Order accepted and awaiting processing", cause);
    this.orderId = orderId;
  }
}
