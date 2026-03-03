package com.kafein.discovery.dto.response;

import java.util.List;
import java.util.UUID;

public record ClassifyResponse(
    UUID columnId,
    String columnName,
    String tableName,
    String databaseName,
    int sampleCount,
    List<String> samples,
    List<ProbabilityItem> classifications
) {
    public record ProbabilityItem(
        String category,
        double probability
    ) {}
}
