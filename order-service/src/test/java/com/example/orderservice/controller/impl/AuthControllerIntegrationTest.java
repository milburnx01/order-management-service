package com.example.orderservice.controller.impl;

import com.example.orderservice.entity.User;
import com.example.orderservice.entity.util.Role;
import com.example.orderservice.repository.UserRepository;
import com.example.orderservice.security.JwtProvider;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class AuthControllerIntegrationTest {

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

    @Autowired
    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void register_ShouldCreateUserAndReturnTokens() throws Exception {
        // given
        String request = """
                {
                    "email": "alex@example.com",
                    "username": "alex",
                    "password": "password123"
                }
                """;

        // when
        String response = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        User savedUser = userRepository.findUserByUsername("alex").orElseThrow();

        assertThat(savedUser.getEmail()).isEqualTo("alex@example.com");
        assertThat(savedUser.getRole()).isEqualTo(Role.USER);
        assertThat(savedUser.getPassword()).isNotEqualTo("password123");
        assertThat(passwordEncoder.matches("password123", savedUser.getPassword()))
                .isTrue();

        String accessToken = JsonPath.read(response, "$.accessToken");
        String refreshToken = JsonPath.read(response, "$.refreshToken");

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(refreshToken)).isTrue();
    }

    @Test
    void register_ShouldReturnBadRequest_WhenUsernameAlreadyExists() throws Exception {
        // given
        User user = new User(
                null,
                "alex",
                passwordEncoder.encode("password123"),
                "alex@example.com",
                Role.USER
        );

        userRepository.saveAndFlush(user);

        String request = """
                {
                    "email": "another@example.com",
                    "username": "alex",
                    "password": "password456"
                }
                """;

        // when
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("User with username or email already exists"));

        // then
        assertThat(userRepository.count()).isEqualTo(1);
    }

    @Test
    void register_ShouldReturnBadRequest_WhenEmailIsInvalid() throws Exception {
        // given
        String request = """
                {
                    "email": "invalid-email",
                    "username": "alex",
                    "password": "password123"
                }
                """;

        // when
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());

        // then
        assertThat(userRepository.count()).isZero();
    }

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() throws Exception {
        // given
        User user = new User(
                null,
                "alex",
                passwordEncoder.encode("password123"),
                "alex@example.com",
                Role.USER
        );

        userRepository.saveAndFlush(user);

        String request = """
                {
                    "username": "alex",
                    "password": "password123"
                }
                """;

        // when
        String response = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        String accessToken = JsonPath.read(response, "$.accessToken");
        String refreshToken = JsonPath.read(response, "$.refreshToken");

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.validateRefreshToken(refreshToken)).isTrue();
        assertThat(jwtProvider.getAccessClaims(accessToken).getSubject())
                .isEqualTo("alex");
    }

    @Test
    void login_ShouldReturnBadRequest_WhenPasswordIsInvalid() throws Exception {
        // given
        User user = new User(
                null,
                "alex",
                passwordEncoder.encode("password123"),
                "alex@example.com",
                Role.USER
        );

        userRepository.saveAndFlush(user);

        String request = """
                {
                    "username": "alex",
                    "password": "wrongPassword"
                }
                """;

        // when
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"));
    }

    @Test
    void login_ShouldReturnBadRequest_WhenUserDoesNotExist() throws Exception {
        // given
        String request = """
                {
                    "username": "unknown",
                    "password": "password123"
                }
                """;

        // when
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value("Invalid username or password"));
    }

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() throws Exception {
        // given
        User user = new User(
                null,
                "alex",
                passwordEncoder.encode("password123"),
                "alex@example.com",
                Role.USER
        );

        User savedUser = userRepository.saveAndFlush(user);
        String refreshToken = jwtProvider.generateRefreshToken(savedUser);

        String request = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        // when
        String response = mockMvc.perform(get("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").value(refreshToken))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // then
        String accessToken = JsonPath.read(response, "$.accessToken");

        assertThat(jwtProvider.validateAccessToken(accessToken)).isTrue();
        assertThat(jwtProvider.getAccessClaims(accessToken).getSubject())
                .isEqualTo("alex");
    }

    @Test
    void refresh_ShouldReturnBadRequest_WhenRefreshTokenIsInvalid() throws Exception {
        // given
        String request = """
                {
                    "refreshToken": "invalid-token"
                }
                """;

        // when
        mockMvc.perform(get("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }
}