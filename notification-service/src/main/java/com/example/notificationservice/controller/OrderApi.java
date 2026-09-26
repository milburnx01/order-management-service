package com.example.notificationservice.controller;

import com.example.notificationservice.dto.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@Tag(name = "Заказы", description = "Read-only API для аналитики")
public interface OrderApi {

    @Operation(summary = "Все заказы", description = "Позволяет получить информацию о всех покупках")
    Page<OrderResponse> findAll(@RequestParam int page, @RequestParam int size);

    @Operation(summary = "Все покупки по заказу", description = "Позволяет найти все покупки, относящиеся к одному заказу")
    Page<OrderResponse> findAllByOrderId(@RequestParam("orderId") UUID orderId, @RequestParam int page, @RequestParam int size);

    @Operation(summary = "Все покупки пользователя", description = "Позволяет найти все покупки конкретного пользователя")
    Page<OrderResponse> findAllByUserId(@RequestParam("userId") UUID userId, @RequestParam int page, @RequestParam int size);
}
