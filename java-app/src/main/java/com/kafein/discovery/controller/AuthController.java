package com.kafein.discovery.controller;

import com.kafein.discovery.config.SecurityConfig;
import com.kafein.discovery.dto.response.AuthResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final SecurityConfig securityConfig;

    @PostMapping("/auth")
    public ResponseEntity<AuthResponse> authenticate(@AuthenticationPrincipal UserDetails user) {
        log.info("User '{}' authenticated successfully", user.getUsername());
        String token = securityConfig.generateToken(user.getUsername());
        return ResponseEntity.ok(new AuthResponse(token));
    }
}
