package com.kafein.discovery.repository;

import com.kafein.discovery.entity.DatabaseMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DatabaseMetadataRepository extends JpaRepository<DatabaseMetadata, UUID> {
    List<DatabaseMetadata> findAllByOrderByCreatedAtDesc();
}
