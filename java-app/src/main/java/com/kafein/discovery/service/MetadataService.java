package com.kafein.discovery.service;

import com.kafein.discovery.dto.response.MetadataDetailResponse;
import com.kafein.discovery.dto.response.MetadataListResponse;
import com.kafein.discovery.entity.ColumnMetadata;
import com.kafein.discovery.entity.DatabaseMetadata;
import com.kafein.discovery.entity.TableMetadata;
import com.kafein.discovery.repository.DatabaseMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MetadataService {

    private final DatabaseMetadataRepository repository;

    @Transactional
    public DatabaseMetadata extractAndStoreMetadata(
            String host, int port, String database, String username, String password) {

        String maskedUser = username.length() > 2 ? username.substring(0, 2) + "***" : "***";
        log.info("Connecting to target database: {}@{}:{} (user: {})", database, host, port, maskedUser);

        String jdbcUrl = String.format("jdbc:postgresql://%s:%d/%s", host, port, database);

        // Test connection
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            log.info("Successfully connected to target database: {}", database);
        } catch (SQLException e) {
            log.error("Failed to connect to target database {}@{}:{}: {}", database, host, port, e.getClass().getSimpleName());
            throw new RuntimeException(
                "Cannot connect to database '" + database + "' at " + host + ":" + port +
                ". Please verify connection details. Error: " + e.getClass().getSimpleName()
            );
        }

        // Create metadata record
        DatabaseMetadata dbMetadata = DatabaseMetadata.builder()
            .databaseName(database)
            .host(host)
            .port(port)
            .username(username)
            .password(password)
            .build();

        // Extract tables and columns using information_schema
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            // Get all tables in public schema
            String tableSql = """
                SELECT table_name FROM information_schema.tables
                WHERE table_schema = 'public' AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """;

            try (PreparedStatement stmt = conn.prepareStatement(tableSql);
                 ResultSet rs = stmt.executeQuery()) {

                while (rs.next()) {
                    String tableName = rs.getString("table_name");

                    TableMetadata tableMetadata = TableMetadata.builder()
                        .database(dbMetadata)
                        .tableName(tableName)
                        .schemaName("public")
                        .build();

                    // Get columns for this table
                    String colSql = """
                        SELECT column_name, data_type, is_nullable, column_default, ordinal_position
                        FROM information_schema.columns
                        WHERE table_schema = 'public' AND table_name = ?
                        ORDER BY ordinal_position
                        """;

                    try (PreparedStatement colStmt = conn.prepareStatement(colSql)) {
                        colStmt.setString(1, tableName);
                        try (ResultSet colRs = colStmt.executeQuery()) {
                            List<ColumnMetadata> columns = new ArrayList<>();
                            while (colRs.next()) {
                                ColumnMetadata col = ColumnMetadata.builder()
                                    .table(tableMetadata)
                                    .columnName(colRs.getString("column_name"))
                                    .dataType(colRs.getString("data_type"))
                                    .isNullable(colRs.getString("is_nullable"))
                                    .columnDefault(colRs.getString("column_default"))
                                    .ordinalPosition(colRs.getInt("ordinal_position"))
                                    .build();
                                columns.add(col);
                            }
                            tableMetadata.setColumns(columns);
                        }
                    }

                    dbMetadata.getTables().add(tableMetadata);
                }
            }

            log.info("Found {} tables in database '{}'", dbMetadata.getTableCount(), database);

        } catch (SQLException e) {
            log.error("Failed to extract metadata from database '{}': {}", database, e.getMessage());
            throw new RuntimeException("Failed to extract metadata from database '" + database + "': " + e.getMessage());
        }

        DatabaseMetadata saved = repository.save(dbMetadata);
        int totalColumns = saved.getTables().stream().mapToInt(t -> t.getColumns().size()).sum();
        log.info("Metadata extraction complete: {} tables, {} columns stored with ID {}",
            saved.getTableCount(), totalColumns, saved.getId());

        return saved;
    }

    @Transactional(readOnly = true)
    public List<MetadataListResponse> listAllMetadata() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
            .map(m -> new MetadataListResponse(
                m.getId(),
                m.getDatabaseName(),
                m.getCreatedAt(),
                m.getTableCount()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<MetadataDetailResponse> getMetadataById(UUID id) {
        return repository.findById(id).map(this::toDetailResponse);
    }

    @Transactional
    public boolean deleteMetadata(UUID id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
            log.info("Deleted metadata record: {} (cascade: tables + columns)", id);
            return true;
        }
        log.warn("Cannot delete: metadata {} not found", id);
        return false;
    }

    private MetadataDetailResponse toDetailResponse(DatabaseMetadata m) {
        List<MetadataDetailResponse.TableResponse> tables = m.getTables().stream()
            .map(t -> new MetadataDetailResponse.TableResponse(
                t.getId(),
                t.getTableName(),
                t.getSchemaName(),
                t.getColumns().stream()
                    .map(c -> new MetadataDetailResponse.ColumnResponse(
                        c.getId(),
                        c.getColumnName(),
                        c.getDataType(),
                        c.getIsNullable(),
                        c.getColumnDefault(),
                        c.getOrdinalPosition()
                    ))
                    .toList()
            ))
            .toList();

        return new MetadataDetailResponse(
            m.getId(),
            m.getDatabaseName(),
            m.getTableCount(),
            tables
        );
    }
}
