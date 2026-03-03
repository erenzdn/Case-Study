package com.kafein.discovery.controller;

import com.kafein.discovery.config.SecurityConfig;
import com.kafein.discovery.dto.response.AuthResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController Unit Tests")
class AuthControllerTest {

    @Mock
    private SecurityConfig securityConfig;

    @InjectMocks
    private AuthController authController;

    private UserDetails adminUser() {
        return User.withUsername("admin")
            .password("encodedPassword")
            .roles("ADMIN")
            .build();
    }

    @Test
    @DisplayName("Successful authentication returns 200 with JWT token")
    void authenticate_validUser_returns200WithToken() {
        // Given
        String expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.test";
        when(securityConfig.generateToken("admin")).thenReturn(expectedToken);

        // When
        ResponseEntity<AuthResponse> response = authController.authenticate(adminUser());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isEqualTo(expectedToken);
    }

    @Test
    @DisplayName("Authentication response contains 'bearer' token type")
    void authenticate_validUser_returnsTokenTypeBeare() {
        // Given
        when(securityConfig.generateToken(anyString())).thenReturn("some.jwt.token");

        // When
        ResponseEntity<AuthResponse> response = authController.authenticate(adminUser());

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().tokenType()).isEqualToIgnoringCase("bearer");
    }

    @Test
    @DisplayName("Token generation is called with the authenticated username")
    void authenticate_callsGenerateTokenWithCorrectUsername() {
        // Given
        when(securityConfig.generateToken("admin")).thenReturn("token");

        // When
        authController.authenticate(adminUser());

        // Then
        verify(securityConfig, times(1)).generateToken("admin");
    }

    @Test
    @DisplayName("Access token in response is non-null and non-empty")
    void authenticate_tokenIsNotNullOrEmpty() {
        // Given
        when(securityConfig.generateToken(anyString())).thenReturn("real.jwt.token");

        // When
        ResponseEntity<AuthResponse> response = authController.authenticate(adminUser());

        // Then
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().accessToken()).isNotBlank();
    }
}
