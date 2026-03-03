package com.kafein.discovery.dto.response;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MetadataListResponse(
    UUID metadataId,
    String databaseName,
    OffsetDateTime createdAt,
    int tableCount
) {}
