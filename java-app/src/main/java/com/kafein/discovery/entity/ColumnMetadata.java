package com.kafein.discovery.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "column_metadata")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
@ToString(exclude = {"table", "classifications"})
public class ColumnMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private TableMetadata table;

    @Column(name = "column_name", nullable = false)
    private String columnName;

    @Column(name = "data_type", nullable = false, length = 100)
    private String dataType;

    @Column(name = "is_nullable", length = 10)
    @Builder.Default
    private String isNullable = "YES";

    @Column(name = "column_default", columnDefinition = "TEXT")
    private String columnDefault;

    @Column(name = "ordinal_position")
    private Integer ordinalPosition;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @JsonIgnore
    @OneToMany(mappedBy = "column", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ClassificationResult> classifications = new ArrayList<>();
}
