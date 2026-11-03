
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void findUserByUsername_ShouldReturnUser_WhenUserExists() {
        // given
        String username = "john";

        User user = new User(
                UUID.randomUUID(),
                username,
                "encodedPassword",
                "john@example.com",
                Role.USER
        );

        when(userRepository.findUserByUsername(username))
                .thenReturn(Optional.of(user));

        // when
        User result = userService.findUserByUsername(username);

        // then
        assertThat(result).isEqualTo(user);

        verify(userRepository).findUserByUsername(username);
    }

    @Test
    void findUserByUsername_ShouldThrowException_WhenUserNotFound() {
        // given
        String username = "unknown";

        when(userRepository.findUserByUsername(username))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUserByUsername(username))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with username " + username + " not found");

        verify(userRepository).findUserByUsername(username);
    }

    @Test
    void createUser_ShouldSaveUserWithUserRole() {
        // given
        CreateUserRequest request = new CreateUserRequest(
                "john@example.com",
                "john",
                "password123"
        );

        User user = new User(
                UUID.randomUUID(),
                "john",
                "encodedPassword",
                "john@example.com",
                Role.USER
        );

        UserResponse response = new UserResponse(
                user.getId().toString(),
                "john",
                "john@example.com",
                Role.USER
        );

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encodedPassword");

        when(userMapper.toUser(
                request.email(),
                request.username(),
                "encodedPassword",
                Role.USER
        )).thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userMapper.toUserResponse(user))
                .thenReturn(response);

        // when
        userService.createUser(request);

        // then
        verify(passwordEncoder).encode(request.password());

        verify(userMapper).toUser(
                request.email(),
                request.username(),
                "encodedPassword",
                Role.USER
        );

        verify(userRepository).saveAndFlush(user);
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void createUser_ShouldThrowAuthException_WhenUserAlreadyExists() {
        // given
        CreateUserRequest request = new CreateUserRequest(
                "john@example.com",
                "john",
                "password123"
        );

        User user = new User(
                UUID.randomUUID(),
                "john",
                "encodedPassword",
                "john@example.com",
                Role.USER
        );

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encodedPassword");

        when(userMapper.toUser(
                request.email(),
                request.username(),
                "encodedPassword",
                Role.USER
        )).thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate username or email"
                ));

        // when & then
        assertThatThrownBy(() -> userService.createUser(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("User with username or email already exists");

        verify(userRepository).saveAndFlush(user);
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    void createUserByAdmin_ShouldReturnCreatedUser() {
        // given
        UUID userId = UUID.randomUUID();

        AdminCreateUserRequest request = new AdminCreateUserRequest(
                "admin",
                "password123",
                "admin@example.com",
                Role.ADMIN
        );

        User user = new User(
                userId,
                "admin",
                "encodedPassword",
                "admin@example.com",
                Role.ADMIN
        );

        UserResponse response = new UserResponse(
                userId.toString(),
                "admin",
                "admin@example.com",
                Role.ADMIN
        );

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encodedPassword");

        when(userMapper.toUser(
                request.email(),
                request.username(),
                "encodedPassword",
                Role.ADMIN
        )).thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenReturn(user);

        when(userMapper.toUserResponse(user))
                .thenReturn(response);

        // when
        UserResponse result = userService.createUserByAdmin(request);

        // then
        assertThat(result).isEqualTo(response);
        assertThat(result.role()).isEqualTo(Role.ADMIN);

        verify(passwordEncoder).encode(request.password());
        verify(userRepository).saveAndFlush(user);
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void createUserByAdmin_ShouldThrowAuthException_WhenUserAlreadyExists() {
        // given
        AdminCreateUserRequest request = new AdminCreateUserRequest(
                "admin",
                "password123",
                "admin@example.com",
                Role.ADMIN
        );

        User user = new User(
                UUID.randomUUID(),
                "admin",
                "encodedPassword",
                "admin@example.com",
                Role.ADMIN
        );

        when(passwordEncoder.encode(request.password()))
                .thenReturn("encodedPassword");

        when(userMapper.toUser(
                request.email(),
                request.username(),
                "encodedPassword",
                Role.ADMIN
        )).thenReturn(user);

        when(userRepository.saveAndFlush(user))
                .thenThrow(new DataIntegrityViolationException(
                        "Duplicate username or email"
                ));

        // when & then
        assertThatThrownBy(() -> userService.createUserByAdmin(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("User with username or email already exists");

        verify(userRepository).saveAndFlush(user);
    }

    @Test
    void findUserById_ShouldReturnUser_WhenUserExists() {
        // given
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "john",
                "encodedPassword",
                "john@example.com",
                Role.USER
        );

        UserResponse response = new UserResponse(
                userId.toString(),
                "john",
                "john@example.com",
                Role.USER
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(userMapper.toUserResponse(user))
                .thenReturn(response);

        // when
        UserResponse result = userService.findUserById(userId);

        // then
        assertThat(result).isEqualTo(response);

        verify(userRepository).findById(userId);
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void findUserById_ShouldThrowException_WhenUserNotFound() {
        // given
        UUID userId = UUID.randomUUID();

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.findUserById(userId))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id " + userId + " not found");

        verify(userRepository).findById(userId);
        verify(userMapper, never()).toUserResponse(any(User.class));
    }

    @Test
    void deleteById_ShouldDeleteUser() {
        // given
        UUID userId = UUID.randomUUID();

        // when
        userService.deleteById(userId);

        // then
        verify(userRepository).deleteById(userId);
    }

    @Test
    void updateUser_ShouldUpdateUserFields() {
        // given
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "oldUsername",
                "oldEncodedPassword",
                "old@example.com",
                Role.USER
        );

        UpdateUserRequest request = new UpdateUserRequest(
                "newUsername",
                "newPassword123",
                "new@example.com",
                Role.ADMIN
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.of(user));

        when(passwordEncoder.encode(request.password()))
                .thenReturn("newEncodedPassword");

        // when
        userService.updateUser(userId, request);

        // then
        assertThat(user.getUsername()).isEqualTo("newUsername");
        assertThat(user.getEmail()).isEqualTo("new@example.com");
        assertThat(user.getPassword()).isEqualTo("newEncodedPassword");
        assertThat(user.getRole()).isEqualTo(Role.ADMIN);

        verify(userRepository).findById(userId);
        verify(passwordEncoder).encode(request.password());
    }

    @Test
    void updateUser_ShouldThrowException_WhenUserNotFound() {
        // given
        UUID userId = UUID.randomUUID();

        UpdateUserRequest request = new UpdateUserRequest(
                "newUsername",
                "newPassword123",
                "new@example.com",
                Role.USER
        );

        when(userRepository.findById(userId))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> userService.updateUser(userId, request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with id " + userId + " not found");

        verify(userRepository).findById(userId);
        verify(passwordEncoder, never()).encode(any(String.class));
    }

    @Test
    void findAll_ShouldReturnPageOfUsers() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        UUID userId = UUID.randomUUID();

        User user = new User(
                userId,
                "john",
                "encodedPassword",
                "john@example.com",
                Role.USER
        );

        UserResponse response = new UserResponse(
                userId.toString(),
                "john",
                "john@example.com",
                Role.USER
        );

        when(userRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        when(userMapper.toUserResponse(user))
                .thenReturn(response);

        // when
        Page<UserResponse> result = userService.findAll(pageable);

        // then
        assertThat(result.getContent()).containsExactly(response);
        assertThat(result.getTotalElements()).isEqualTo(1);
        assertThat(result.getTotalPages()).isEqualTo(1);

        verify(userRepository).findAll(pageable);
        verify(userMapper).toUserResponse(user);
    }

    @Test
    void findAll_ShouldReturnEmptyPage_WhenNoUsersExist() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        when(userRepository.findAll(pageable))
                .thenReturn(Page.empty(pageable));

        // when
        Page<UserResponse> result = userService.findAll(pageable);

        // then
        assertThat(result).isEmpty();
        assertThat(result.getTotalElements()).isZero();

        verify(userRepository).findAll(pageable);
    }
}
