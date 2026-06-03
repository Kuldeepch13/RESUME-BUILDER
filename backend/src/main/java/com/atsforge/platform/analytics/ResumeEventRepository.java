package com.atsforge.platform.analytics;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeEventRepository extends JpaRepository<ResumeEventEntity, UUID> {
    long countByResumeIdAndEventType(UUID resumeId, String eventType);
}

