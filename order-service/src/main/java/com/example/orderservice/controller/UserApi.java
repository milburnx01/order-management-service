package com.example.orderservice.controller;

import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

@Tag(name = "Пользователи", description = "Управление данними пользователей")
public interface UserApi {

    @Operation(summary = "Создание пользователя администратором")
    ResponseEntity<UserResponse> createUserByAdmin(AdminCreateUserRequest adminCreateUserRequest);

    @Operation(summary = "Найти пользователя", description = "Позволяет найти пользователя по его id")
    ResponseEntity<UserResponse> findById(UUID id);

    @Operation(summary = "Удаление пользователя", description = "Позволяет удалить пользователя по его id")
    ResponseEntity<Void> deleteById(UUID id);

    @Operation(summary = "Обновление пользователя", description = "Позволяет обновить пользователя по его id")
    ResponseEntity<Void> updateUser(UUID id, UpdateUserRequest createUserRequest);

    @Operation(summary = "Все пользователи", description = "Позволяет получить список всех пользователей")
    ResponseEntity<Page<UserResponse>> findAll(int page, int size);
}
