package com.kafein.discovery.dto.response;

public record AuthResponse(
    String accessToken,
    String tokenType
) {
    public AuthResponse(String accessToken) {
        this(accessToken, "bearer");
    }
}
