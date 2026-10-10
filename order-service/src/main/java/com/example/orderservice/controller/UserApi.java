package com.example.orderservice.controller;

import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

public interface UserApi {

    ResponseEntity<UserResponse> createUserByAdmin(AdminCreateUserRequest adminCreateUserRequest);

    ResponseEntity<UserResponse> findById(UUID id);

    ResponseEntity<Void> deleteById(UUID id);

    ResponseEntity<Void> updateUser(UUID id, UpdateUserRequest createUserRequest);

    ResponseEntity<Page<UserResponse>> findAll(int page, int size);
}
