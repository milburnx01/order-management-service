package com.example.orderservice.mapper;

import com.example.orderservice.dto.OrderResponse;
import com.example.orderservice.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mapping(source = "id", target = "orderId")
    OrderResponse toOrderResponse(Order order);
}