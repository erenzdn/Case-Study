package com.kafein.discovery.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores target database connection info and its extracted metadata.
 * Password is stored (encrypted in prod) and never serialized to JSON.
 */
@Entity
@Table(name = "database_metadata")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
// Exclude sensitive fields from toString() to prevent credential leakage in logs
@ToString(exclude = {"password", "tables"})
public class DatabaseMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "database_name", nullable = false)
    private String databaseName;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private Integer port;

    @Column(nullable = false)
    private String username;

    // Intentionally excluded from JSON responses — security requirement (SKİLLS.md §2)
    @JsonIgnore
    @Column(nullable = false, columnDefinition = "TEXT")
    private String password;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @OneToMany(mappedBy = "database", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<TableMetadata> tables = new ArrayList<>();

    public int getTableCount() {
        return tables != null ? tables.size() : 0;
    }
}
