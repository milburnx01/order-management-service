
package com.example.orderservice.kafka;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.OutboxEvent;
import com.example.orderservice.entity.util.OutboxStatus;
import com.example.orderservice.repository.OutboxEventRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxPublisher {

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, OrderResponse> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events = outboxEventRepository.findByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

        for (OutboxEvent event : events) {
            try {
                publishEvent(event);
            } catch (Exception e) {
                log.error("Failed to publish outbox event {}", event.getId(), e);
            }
        }
    }

    private void publishEvent(OutboxEvent event) {

        OrderResponse response = objectMapper.readValue(event.getPayload(), OrderResponse.class);

        kafkaTemplate.send(event.getTopic(), event.getOrderId().toString(), response).join();

        event.setStatus(OutboxStatus.SENT);
        outboxEventRepository.save(event);

        log.info("Outbox event {} published successfully", event.getId());
    }
}
