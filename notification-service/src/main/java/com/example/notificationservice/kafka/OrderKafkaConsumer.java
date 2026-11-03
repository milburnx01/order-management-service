package com.example.notificationservice.kafka;

import com.example.notificationservice.dto.OrderCreatedEvent;
import com.example.notificationservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderKafkaConsumer {

    private final OrderService orderService;

    @KafkaListener(topics = "orders")
    public void consumeOrder(OrderCreatedEvent orderCreatedEvent) {
        orderService.createOrder(orderCreatedEvent);
    }

}
