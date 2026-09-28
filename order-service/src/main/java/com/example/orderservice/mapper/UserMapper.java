package com.example.orderservice.mapper;

import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    User toUser(CreateUserRequest createUserRequest);
}
