package com.kafein.discovery.service;

import com.kafein.discovery.dto.response.ClassifyResponse;
import com.kafein.discovery.entity.ColumnMetadata;
import com.kafein.discovery.entity.DatabaseMetadata;
import com.kafein.discovery.entity.TableMetadata;
import com.kafein.discovery.repository.ColumnMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClassifyService {

    private final ColumnMetadataRepository columnRepository;
    private final LlmService llmService;

    @Transactional(readOnly = true)
    public ClassifyResponse classifyColumn(UUID columnId, int sampleCount) {
        log.info("Starting classification for column_id={}, sample_count={}", columnId, sampleCount);

        // Step 1: Find column metadata
        ColumnMetadata column = columnRepository.findById(columnId)
            .orElseThrow(() -> {
                log.warn("Column not found: {}", columnId);
                return new IllegalArgumentException("Column with ID " + columnId + " not found");
            });

        TableMetadata table = column.getTable();
        DatabaseMetadata database = table.getDatabase();

        log.info("Found column: {}.{}.{} (type: {})",
            database.getDatabaseName(), table.getTableName(), column.getColumnName(), column.getDataType());

        // Step 2: Connect to target DB and extract sample data
        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s",
            database.getHost(), database.getPort(), database.getDatabaseName());

        List<String> samples = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, database.getUsername(), database.getPassword())) {
            String query = String.format(
                "SELECT \"%s\" FROM \"%s\".\"%s\" WHERE \"%s\" IS NOT NULL LIMIT ?",
                column.getColumnName(), table.getSchemaName(), table.getTableName(), column.getColumnName()
            );
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, sampleCount);
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        samples.add(rs.getString(1));
                    }
                }
            }
            log.info("Extracted {} samples from {}.{}", samples.size(), table.getTableName(), column.getColumnName());
        } catch (SQLException e) {
            log.error("Failed to extract samples from target DB: {}", e.getMessage());
            throw new RuntimeException(
                "Failed to extract sample data from column '" + column.getColumnName() +
                "' in table '" + table.getTableName() + "': " + e.getMessage()
            );
        }

        if (samples.isEmpty()) {
            throw new IllegalArgumentException(
                "No non-null data found in column '" + column.getColumnName() +
                "' of table '" + table.getTableName() + "'"
            );
        }

        // Step 3: Classify using LLM
        log.info("Sending {} samples to LLM for classification...", samples.size());
        List<Map<String, Object>> classifications = llmService.classifyWithLlm(
            column.getColumnName(), table.getTableName(), column.getDataType(), samples
        );

        // Step 4: Build response
        List<ClassifyResponse.ProbabilityItem> items = classifications.stream()
            .map(c -> new ClassifyResponse.ProbabilityItem(
                (String) c.get("category"),
                ((Number) c.get("probability")).doubleValue()
            ))
            .toList();

        return new ClassifyResponse(
            column.getId(),
            column.getColumnName(),
            table.getTableName(),
            database.getDatabaseName(),
            samples.size(),
            samples,
            items
        );
    }
}
