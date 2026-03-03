package com.kafein.discovery.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record DBConnectionRequest(
    @NotBlank(message = "Host is required")
    String host,

    @Positive(message = "Port must be positive")
    Integer port,

    @NotBlank(message = "Database name is required")
    String database,

    @NotBlank(message = "Username is required")
    String username,

    @NotBlank(message = "Password is required")
    String password
) {
    public DBConnectionRequest {
        if (port == null) port = 5432;
    }
}
