package com.kafein.discovery.dto.response;

import java.util.List;
import java.util.UUID;

public record MetadataDetailResponse(
    UUID metadataId,
    String databaseName,
    int tableCount,
    List<TableResponse> tables
) {
    public record TableResponse(
        UUID tableId,
        String tableName,
        String schemaName,
        List<ColumnResponse> columns
    ) {}

    public record ColumnResponse(
        UUID columnId,
        String columnName,
        String dataType,
        String isNullable,
        String columnDefault,
        Integer ordinalPosition
    ) {}
}
