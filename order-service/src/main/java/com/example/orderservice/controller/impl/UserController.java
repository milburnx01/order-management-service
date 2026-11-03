package com.example.orderservice.controller.impl;

import com.example.orderservice.controller.UserApi;
import com.example.orderservice.dto.AdminCreateUserRequest;
import com.example.orderservice.dto.UpdateUserRequest;
import com.example.orderservice.dto.UserResponse;
import com.example.orderservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController implements UserApi {

    private final UserService userService;

    @GetMapping
    @Override
    public ResponseEntity<Page<UserResponse>> findAll(@RequestParam(value = "page", required = false, defaultValue = "0") int page,
                                                      @RequestParam(value = "value", required = false, defaultValue = "10")  int size) {
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(userService.findAll(pageable));
    }

    @GetMapping("/{id}")
    @Override
    public ResponseEntity<UserResponse> findById(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(userService.findUserById(id));
    }

    @PostMapping
    @Override
    public ResponseEntity<UserResponse> createUserByAdmin(@RequestBody @Valid AdminCreateUserRequest adminCreateUserRequest) {
        UserResponse userResponse = userService.createUserByAdmin(adminCreateUserRequest);
        URI location = UriComponentsBuilder
                .fromPath("/api/users/{id}")
                .buildAndExpand(userResponse.id())
                .toUri();
        return ResponseEntity.created(location).body(userResponse);
    }

    @PutMapping("/{id}")
    @Override
    public ResponseEntity<Void> updateUser(@PathVariable("id") UUID id, @RequestBody @Valid UpdateUserRequest createUserRequest) {
        userService.updateUser(id, createUserRequest);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @Override
    public ResponseEntity<Void> deleteById(@PathVariable("id") UUID id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
