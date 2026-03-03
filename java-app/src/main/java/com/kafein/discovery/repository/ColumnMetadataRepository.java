package com.kafein.discovery.repository;

import com.kafein.discovery.entity.ColumnMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ColumnMetadataRepository extends JpaRepository<ColumnMetadata, UUID> {
}
