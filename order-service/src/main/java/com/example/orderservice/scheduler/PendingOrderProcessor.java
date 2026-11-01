package com.example.orderservice.scheduler;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.Order;
import com.example.orderservice.entity.util.OrderStatus;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.mapper.OrderMapper;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.OrderPersistenceService;

import com.example.orderservice.tracing.StoredTraceContext;
import io.grpc.StatusRuntimeException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingOrderProcessor {

    private final OrderRepository orderRepository;
    private final OrderPersistenceService orderPersistenceService;
    private final InventoryClient inventoryClient;
    private final OrderMapper orderMapper;
    private final StoredTraceContext traceContext;

    @Scheduled(fixedDelay = 30000)
    public void processPendingOrders() {
        List<Order> pendingOrders = orderRepository.findByStatus(OrderStatus.PENDING);

        for (Order order : pendingOrders) {
            try {
                traceContext.run(order.getTraceContext(),
                        "order.retry",
                        () -> {
                            try {
                                processOrder(order);
                            } catch (StatusRuntimeException e) {
                                handleGrpcError(order, e);
                            }
                        }
                );
            } catch (Exception e) {
                log.error("Unexpected error processing order {}", order.getId(), e);
            }
        }
    }

    private void processOrder(Order order) {

        List<ProductItem> items = order.getItems()
                .stream()
                .map(item -> new ProductItem(
                        item.getProductId().toString(),
                        item.getQuantity()
                ))
                .toList();

        inventoryClient.writeOffProducts(items, order.getId());
        OrderResponse response = orderMapper.toOrderResponse(order);
        orderPersistenceService.markOrderCreated(order.getId(), response);
        log.info("Order {} successfully processed", order.getId());
    }

    private void handleGrpcError(Order order, StatusRuntimeException e) {
        switch (e.getStatus().getCode()) {
            case FAILED_PRECONDITION, NOT_FOUND, INVALID_ARGUMENT -> {
                orderPersistenceService.markOrderFailed(order.getId());
                log.warn("Order {} failed permanently: {}", order.getId(), e.getStatus().getCode());
            }
            default -> log.warn("Order {} remains PENDING for retry: {}", order.getId(), e.getStatus().getCode());
        }
    }
}
