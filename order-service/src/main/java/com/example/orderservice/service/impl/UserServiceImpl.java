package com.example.orderservice.service.impl;

import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import com.example.orderservice.entity.User;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.exception.AuthException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.mapper.UserMapper;
import com.example.orderservice.repository.UserRepository;
import com.example.orderservice.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findUserByUsername(String username) {
        log.info("Finding user by username={}", username);
        User user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
        log.info("User found successfully, username={}", username);
        return user;
    }

    @Override
    public void createUser(CreateUserRequest createUserRequest) {
        log.info("Creating user, username={}, email={}", createUserRequest.username(), createUserRequest.email());
        createUser(createUserRequest.email(),
                createUserRequest.username(),
                createUserRequest.password(),
                Role.USER);
        log.info("User created successfully, username={}",
                createUserRequest.username());
    }

    @Override
    public UserResponse createUserByAdmin(AdminCreateUserRequest adminCreateUserRequest) {
        log.info("Admin creating user, username={}, role={}", adminCreateUserRequest.username(), adminCreateUserRequest.role());
        UserResponse response = createUser(adminCreateUserRequest.email(),
                adminCreateUserRequest.username(),
                adminCreateUserRequest.password(),
                adminCreateUserRequest.role());
        log.info("User created by admin successfully, username={}", adminCreateUserRequest.username());
        return response;
    }

    @Override
    public UserResponse findUserById(UUID id) {
        log.info("Finding user by id={}", id);
        UserResponse response = userRepository.findById(id).map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
        log.info("User found successfully, id={}", id);
        return response;
    }

    @Override
    public void deleteById(UUID id) {
        log.info("Deleting user, id={}", id);
        userRepository.deleteById(id);
        log.info("User deleted successfully, id={}", id);
    }

    @Transactional
    @Override
    public void updateUser(UUID id, UpdateUserRequest updateUserRequest) {
        log.info("Updating user, id={}, username={}, role={}", id, updateUserRequest.username(), updateUserRequest.role());
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
        user.setUsername(updateUserRequest.username());
        user.setEmail(updateUserRequest.email());
        user.setPassword(passwordEncoder.encode(updateUserRequest.password()));
        user.setRole(updateUserRequest.role());
        log.info("User update completed, id={}", id);
    }

    @Override
    public Page<UserResponse> findAll(Pageable pageable) {
        log.info("Finding all users, pageable={}", pageable);
        Page<UserResponse> response = userRepository.findAll(pageable)
                .map(userMapper::toUserResponse);
        log.info("Users found successfully, totalElements={}",
                response.getTotalElements());
        return response;
    }

    private UserResponse createUser(String email, String username, String password, Role role) {
        User user = userMapper.toUser(email, username, passwordEncoder.encode(password), role);
        try {
            User savedUser = userRepository.saveAndFlush(user);
            return userMapper.toUserResponse(savedUser);
        } catch (DataIntegrityViolationException e) {
            throw new AuthException("User with username or email already exists");
        }
    }

}
