
package com.example.orderservice.controller.impl;

import com.example.orderservice.entity.User;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.repository.UserRepository;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
@WithMockUser(username = "admin", authorities = "ADMIN")
class UserControllerIntegrationTest {

    @Container
    private static final PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    @SneakyThrows
    void createUser_ShouldReturnCreatedAndSaveInDatabase() {
        // given
        String request = """
                {
                    "username": "user",
                    "password": "password123",
                    "email": "user@example.com",
                    "role": "USER"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        assertThat(userRepository.findAll())
                .hasSize(1)
                .first()
                .satisfies(user -> {
                    assertThat(user.getUsername()).isEqualTo("user");
                    assertThat(user.getEmail()).isEqualTo("user@example.com");
                    assertThat(user.getRole()).isEqualTo(Role.USER);
                    assertThat(passwordEncoder.matches(
                            "password123", user.getPassword()
                    )).isTrue();
                });
    }

    @Test
    @SneakyThrows
    void findById_ShouldReturnUser_WhenUserExists() {
        // given
        User user = userRepository.save(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));

        // when & then
        mockMvc.perform(get("/api/users/{id}", user.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(user.getId().toString()))
                .andExpect(jsonPath("$.username").value("user"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @SneakyThrows
    void findAll_ShouldReturnPageOfUsers() {
        // given
        userRepository.save(new User(
                null,
                "user",
                "encodedPassword",
                "john@example.com",
                Role.USER
        ));

        userRepository.save(new User(
                null,
                "user1",
                "encodedPassword",
                "user1@example.com",
                Role.ADMIN
        ));

        // when & then
        mockMvc.perform(get("/api/users")
                        .param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    @SneakyThrows
    void updateUser_ShouldUpdateUserInDatabase() {
        // given
        User user = userRepository.save(new User(
                null,
                "user",
                "oldEncodedPassword",
                "user@example.com",
                Role.USER
        ));

        UUID userId = user.getId();

        String request = """
                {
                    "username": "user_updated",
                    "password": "newPassword123",
                    "email": "user.updated@example.com",
                    "role": "ADMIN"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isNoContent());

        User updatedUser = userRepository.findById(userId).orElseThrow();

        assertThat(updatedUser.getUsername())
                .isEqualTo("user_updated");
        assertThat(updatedUser.getEmail())
                .isEqualTo("user.updated@example.com");
        assertThat(updatedUser.getRole()).isEqualTo(Role.ADMIN);
        assertThat(passwordEncoder.matches(
                "newPassword123", updatedUser.getPassword()
        )).isTrue();
    }

    @Test
    @SneakyThrows
    void deleteUser_ShouldReturnNoContentAndDeleteFromDatabase() {
        // given
        User user = userRepository.save(new User(
                null,
                "user",
                "encodedPassword",
                "user@example.com",
                Role.USER
        ));

        UUID userId = user.getId();

        // when & then
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(userId)).isFalse();
    }

    @Test
    @SneakyThrows
    void createUser_ShouldReturnBadRequest_WhenRequestIsInvalid() {
        // given
        String request = """
                {
                    "username": "",
                    "password": "",
                    "email": "invalid-email",
                    "role": null
                }
                """;

        // when & then
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.count()).isZero();
    }

    @Test
    @SneakyThrows
    @WithMockUser(username = "user", authorities = "USER")
    void findAll_ShouldReturnForbidden_WhenUserIsNotAdmin() {
        // when & then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }
}
