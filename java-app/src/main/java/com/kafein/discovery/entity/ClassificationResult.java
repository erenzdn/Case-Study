package com.kafein.discovery.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "classification_results")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ClassificationResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "column_id", nullable = false)
    private ColumnMetadata column;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "classification", nullable = false, columnDefinition = "jsonb")
    private String classification;

    @CreationTimestamp
    @Column(name = "created_at")
    private OffsetDateTime createdAt;
}
