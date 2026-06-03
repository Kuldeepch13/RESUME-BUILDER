package com.atsforge.platform.resume;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ResumeRepository extends JpaRepository<ResumeEntity, UUID> {
    Page<ResumeEntity> findAllByOwnerId(UUID ownerId, Pageable pageable);
    Optional<ResumeEntity> findByIdAndOwnerId(UUID id, UUID ownerId);
    @Query("SELECT r FROM ResumeEntity r WHERE r.owner.id = ?1 AND (LOWER(r.title) LIKE LOWER(CONCAT('%', ?2, '%')) OR LOWER(r.targetRole) LIKE LOWER(CONCAT('%', ?2, '%')))")
    Page<ResumeEntity> searchByTitleOrRole(UUID ownerId, String query, Pageable pageable);
    Page<ResumeEntity> findAllByOwnerIdAndStatus(UUID ownerId, ResumeEntity.Status status, Pageable pageable);
    Page<ResumeEntity> findAllByOwnerIdAndTemplateId(UUID ownerId, UUID templateId, Pageable pageable);
}

