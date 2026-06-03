package com.atsforge.platform.ai;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysisEntity, UUID> {
    Optional<AiAnalysisEntity> findByIdAndRequestedById(UUID id, UUID userId);
    long countByRequestedByIdAndCreatedAtAfter(UUID userId, Instant createdAfter);
}

