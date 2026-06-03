package com.atsforge.platform.analytics;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeShareRepository extends JpaRepository<ResumeShareEntity, UUID> {
    Optional<ResumeShareEntity> findByPublicToken(String token);
    Optional<ResumeShareEntity> findByIdAndResumeOwnerId(UUID id, UUID ownerId);
}

