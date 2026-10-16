package com.example.orderservice.service.impl;

import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.dto.OrderItemResponse;
import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.dto.ProductItem;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.OrderException;
import com.example.orderservice.grpc.InventoryClient;
import com.example.orderservice.grpc.ProductAvailability;
import com.example.orderservice.kafka.OrderKafkaProducer;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final InventoryClient inventoryClient;
    private final UserService userService;
    private final OrderKafkaProducer orderKafkaProducer;

    @Override
    public OrderResponse createOrder(CreateOrderRequest createOrderRequest) {
        log.info("Creating order, items={}", createOrderRequest.items());
        List<ProductAvailability> productAvailabilities = inventoryClient.checkAvailability(createOrderRequest);
        List<String> unavailableProductIds = new ArrayList<>();
        for (ProductAvailability productAvailability : productAvailabilities) {
            if (!productAvailability.getIsAvailable()) {
                unavailableProductIds.add(productAvailability.getId());
            }
        }
        if (!unavailableProductIds.isEmpty()) {
            throw new OrderException("Products not available: " + String.join(", ", unavailableProductIds));
        }

        UUID orderId = UUID.randomUUID();
        inventoryClient.writeOffProductProducts(createOrderRequest.items());
        User user = userService.findUserByUsername(SecurityContextHolder.getContext().getAuthentication().getName());
        List<OrderItemResponse> items = new ArrayList<>();
        for (int i = 0; i < productAvailabilities.size(); i++) {
            ProductAvailability availability = productAvailabilities.get(i);
            ProductItem productItem = createOrderRequest.items().get(i);

            BigDecimal price = BigDecimal.valueOf(availability.getPrice());
            BigDecimal sale = BigDecimal.valueOf(availability.getSale());

            BigDecimal totalPrice = price
                    .multiply(BigDecimal.valueOf(100).subtract(sale))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(productItem.quantity()))
                    .setScale(2, RoundingMode.HALF_UP);

            items.add(OrderItemResponse.builder()
                    .productId(UUID.fromString(productItem.id()))
                    .productName(availability.getName())
                    .quantity(productItem.quantity())
                    .price(price)
                    .sale(sale)
                    .totalPrice(totalPrice)
                    .build());
        }
        OrderResponse orderResponse = OrderResponse.builder()
                .orderId(orderId)
                .userId(user.getId())
                .items(items)
                .totalPrice(items.stream().map(OrderItemResponse::totalPrice)
                        .reduce(BigDecimal.ZERO, BigDecimal::add))
                .build();

        orderKafkaProducer.sendOrder(orderResponse);
        log.info(
                "Order created successfully, orderId={}, userId={}, totalPrice={}",
                orderResponse.orderId(),
                orderResponse.userId(),
                orderResponse.totalPrice()
        );
        return orderResponse;
    }
}
