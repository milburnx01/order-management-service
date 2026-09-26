package com.example.notificationservice.mapper;

import com.example.notificationservice.dto.OrderResponse;
import com.example.notificationservice.entity.Order;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface OrderMapper {
    OrderResponse toOrderResponse(Order order);
}
