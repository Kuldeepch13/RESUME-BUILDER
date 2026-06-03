package com.atsforge.platform.export;

import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExportJobRepository extends JpaRepository<ExportJobEntity, UUID> {
    long countByRequestedByIdAndCreatedAtAfter(UUID userId, Instant after);
}

