package com.kafein.discovery.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ClassifyRequest(
    @NotNull(message = "Column ID is required")
    UUID columnId,

    @Min(value = 1, message = "Sample count must be at least 1")
    @Max(value = 100, message = "Sample count must not exceed 100")
    Integer sampleCount
) {
    public ClassifyRequest {
        if (sampleCount == null) sampleCount = 10;
    }
}
