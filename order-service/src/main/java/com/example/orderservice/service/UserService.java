package com.example.orderservice.service;

import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import com.example.orderservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserService {

    User findUserByUsername(String username);

    void createUser(CreateUserRequest createUserRequest);

    UserResponse createUserByAdmin(AdminCreateUserRequest adminCreateUserRequest);

    UserResponse findUserById(UUID id);

    void deleteById(UUID id);

    void updateUser(UUID id, UpdateUserRequest updateUserRequest);

    Page<UserResponse> findAll(Pageable pageable);

}
