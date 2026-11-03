
package com.example.orderservice.service.impl;

import com.example.orderservice.dto.JwtRequest;
import com.example.orderservice.dto.JwtResponse;
import com.example.orderservice.dto.RefreshTokenRequest;
import com.example.orderservice.entity.User;
import com.example.orderservice.exception.AuthException;
import com.example.orderservice.exception.UserNotFoundException;
import com.example.orderservice.security.JwtProvider;
import com.example.orderservice.service.UserService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() {
        // given
        JwtRequest request = new JwtRequest("john", "password123");

        User user = new User();
        user.setUsername("john");
        user.setPassword("encodedPassword");

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        when(passwordEncoder.matches("password123", "encodedPassword"))
                .thenReturn(true);

        when(jwtProvider.generateAccessToken(user))
                .thenReturn("accessToken");

        when(jwtProvider.generateRefreshToken(user))
                .thenReturn("refreshToken");

        // when
        JwtResponse result = authService.login(request);

        // then
        assertThat(result.accessToken()).isEqualTo("accessToken");
        assertThat(result.refreshToken()).isEqualTo("refreshToken");

        verify(userService).findUserByUsername("john");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtProvider).generateAccessToken(user);
        verify(jwtProvider).generateRefreshToken(user);
    }

    @Test
    void login_ShouldThrowAuthException_WhenPasswordIsInvalid() {
        // given
        JwtRequest request = new JwtRequest("john", "wrongPassword");

        User user = new User();
        user.setUsername("john");
        user.setPassword("encodedPassword");

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        when(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid username or password");

        verify(userService).findUserByUsername("john");
        verify(passwordEncoder).matches("wrongPassword", "encodedPassword");
        verify(jwtProvider, never()).generateAccessToken(any(User.class));
        verify(jwtProvider, never()).generateRefreshToken(any(User.class));
    }

    @Test
    void login_ShouldThrowAuthException_WhenUserNotFound() {
        // given
        JwtRequest request = new JwtRequest("unknown", "password123");

        when(userService.findUserByUsername("unknown"))
                .thenThrow(new UserNotFoundException(
                        "User with username unknown not found"
                ));

        // when & then
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid username or password");

        verify(userService).findUserByUsername("unknown");
        verify(passwordEncoder, never()).matches(any(), any());
        verify(jwtProvider, never()).generateAccessToken(any(User.class));
    }

    @Test
    void getAccessToken_ShouldReturnNewAccessToken_WhenRefreshTokenIsValid() {
        // given
        RefreshTokenRequest request =
                new RefreshTokenRequest("validRefreshToken");

        Claims claims = org.mockito.Mockito.mock(Claims.class);

        User user = new User();
        user.setUsername("john");

        when(jwtProvider.validateRefreshToken("validRefreshToken"))
                .thenReturn(true);

        when(jwtProvider.getRefreshClaims("validRefreshToken"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("john");

        when(userService.findUserByUsername("john"))
                .thenReturn(user);

        when(jwtProvider.generateAccessToken(user))
                .thenReturn("newAccessToken");

        // when
        JwtResponse result = authService.getAccessToken(request);

        // then
        assertThat(result.accessToken()).isEqualTo("newAccessToken");
        assertThat(result.refreshToken()).isEqualTo("validRefreshToken");

        verify(jwtProvider).validateRefreshToken("validRefreshToken");
        verify(jwtProvider).getRefreshClaims("validRefreshToken");
        verify(userService).findUserByUsername("john");
        verify(jwtProvider).generateAccessToken(user);
        verify(jwtProvider, never()).generateRefreshToken(any(User.class));
    }

    @Test
    void getAccessToken_ShouldThrowAuthException_WhenRefreshTokenIsInvalid() {
        // given
        RefreshTokenRequest request =
                new RefreshTokenRequest("invalidRefreshToken");

        when(jwtProvider.validateRefreshToken("invalidRefreshToken"))
                .thenReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid refresh token");

        verify(jwtProvider).validateRefreshToken("invalidRefreshToken");
        verify(jwtProvider, never()).getRefreshClaims(any());
        verify(jwtProvider, never()).generateAccessToken(any(User.class));
    }

    @Test
    void getAccessToken_ShouldThrowAuthException_WhenRefreshTokenIsNull() {
        // given
        RefreshTokenRequest request = new RefreshTokenRequest(null);

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(request))
                .isInstanceOf(AuthException.class)
                .hasMessage("Invalid refresh token");

        verify(jwtProvider, never()).validateRefreshToken(any());
        verify(jwtProvider, never()).getRefreshClaims(any());
    }

    @Test
    void getAccessToken_ShouldThrowException_WhenUserNotFound() {
        // given
        RefreshTokenRequest request =
                new RefreshTokenRequest("validRefreshToken");

        Claims claims = org.mockito.Mockito.mock(Claims.class);

        when(jwtProvider.validateRefreshToken("validRefreshToken"))
                .thenReturn(true);

        when(jwtProvider.getRefreshClaims("validRefreshToken"))
                .thenReturn(claims);

        when(claims.getSubject())
                .thenReturn("unknown");

        when(userService.findUserByUsername("unknown"))
                .thenThrow(new UserNotFoundException(
                        "User with username unknown not found"
                ));

        // when & then
        assertThatThrownBy(() -> authService.getAccessToken(request))
                .isInstanceOf(UserNotFoundException.class)
                .hasMessage("User with username unknown not found");

        verify(jwtProvider).validateRefreshToken("validRefreshToken");
        verify(jwtProvider).getRefreshClaims("validRefreshToken");
        verify(userService).findUserByUsername("unknown");
        verify(jwtProvider, never()).generateAccessToken(any(User.class));
    }
}
