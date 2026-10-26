package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderKafkaProducer {

    private final KafkaTemplate<String, OrderResponse> kafkaTemplate;

    public void sendOrder(OrderResponse orderResponse) {
        kafkaTemplate.send("orders", orderResponse);
    }
}
