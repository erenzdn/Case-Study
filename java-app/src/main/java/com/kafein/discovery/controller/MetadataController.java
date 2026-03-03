package com.kafein.discovery.controller;

import com.kafein.discovery.dto.request.DBConnectionRequest;
import com.kafein.discovery.dto.response.MetadataDetailResponse;
import com.kafein.discovery.dto.response.MetadataListResponse;
import com.kafein.discovery.entity.DatabaseMetadata;
import com.kafein.discovery.service.MetadataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Slf4j
public class MetadataController {

    private final MetadataService metadataService;

    @PostMapping("/db/metadata")
    public ResponseEntity<Map<String, Object>> extractMetadata(@Valid @RequestBody DBConnectionRequest request) {
        log.info("Received metadata extraction request for database: {}", request.database());

        try {
            DatabaseMetadata metadata = metadataService.extractAndStoreMetadata(
                request.host(), request.port(), request.database(),
                request.username(), request.password()
            );

            // Build response matching Python format
            var tables = metadata.getTables().stream()
                .map(t -> {
                    var columns = t.getColumns().stream()
                        .map(c -> Map.<String, Object>of(
                            "column_id", c.getId(),
                            "column_name", c.getColumnName(),
                            "data_type", c.getDataType(),
                            "is_nullable", c.getIsNullable(),
                            "ordinal_position", c.getOrdinalPosition() != null ? c.getOrdinalPosition() : 0
                        ))
                        .toList();

                    return Map.<String, Object>of(
                        "table_id", t.getId(),
                        "table_name", t.getTableName(),
                        "schema_name", t.getSchemaName(),
                        "columns", columns
                    );
                })
                .toList();

            Map<String, Object> response = Map.of(
                "metadata_id", metadata.getId(),
                "database_name", metadata.getDatabaseName(),
                "table_count", metadata.getTableCount(),
                "tables", tables
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            log.error("Metadata extraction failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/metadata")
    public ResponseEntity<List<MetadataListResponse>> listMetadata() {
        return ResponseEntity.ok(metadataService.listAllMetadata());
    }

    @GetMapping("/metadata/{metadataId}")
    public ResponseEntity<MetadataDetailResponse> getMetadataDetail(@PathVariable UUID metadataId) {
        return metadataService.getMetadataById(metadataId)
            .map(ResponseEntity::ok)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Metadata with ID " + metadataId + " not found"
            ));
    }

    @DeleteMapping("/metadata/{metadataId}")
    public ResponseEntity<Map<String, String>> deleteMetadata(@PathVariable UUID metadataId) {
        if (metadataService.deleteMetadata(metadataId)) {
            return ResponseEntity.ok(Map.of("message", "Metadata deleted successfully"));
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Metadata with ID " + metadataId + " not found");
    }
}
