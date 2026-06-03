package com.atsforge.platform.resume;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersionEntity, UUID> {
    List<ResumeVersionEntity> findAllByResumeIdOrderByVersionNumberDesc(UUID resumeId);
}

