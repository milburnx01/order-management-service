package com.example.orderservice.service.impl;

import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.CreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.exception.AuthException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.entity.User;
import com.example.orderservice.mapper.UserMapper;
import com.example.orderservice.repository.UserRepository;
import com.example.orderservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User findUserByUsername(String username) {
        return userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User with username " + username + " not found"));
    }

    @Override
    public void createUser(CreateUserRequest createUserRequest) {
        createUser(createUserRequest.email(),
                createUserRequest.username(),
                createUserRequest.password(),
                Role.USER);
    }

    @Override
    public UserResponse createUserByAdmin(AdminCreateUserRequest adminCreateUserRequest) {
        return createUser(adminCreateUserRequest.email(),
                adminCreateUserRequest.username(),
                adminCreateUserRequest.password(),
                adminCreateUserRequest.role());
    }

    @Override
    public UserResponse findUserById(UUID id) {
        return userRepository.findById(id).map(userMapper::toUserResponse)
                .orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
    }

    @Override
    public void deleteById(UUID id) {
        userRepository.deleteById(id);
    }

    @Transactional
    @Override
    public void updateUser(UUID id, UpdateUserRequest updateUserRequest) {
        User user = userRepository.findById(id).orElseThrow(() -> new UserNotFoundException("User with id " + id + " not found"));
        user.setUsername(updateUserRequest.username());
        user.setEmail(updateUserRequest.email());
        user.setPassword(passwordEncoder.encode(updateUserRequest.password()));
        user.setRole(updateUserRequest.role());
    }

    @Override
    public Page<UserResponse> findAll(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(userMapper::toUserResponse);
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
