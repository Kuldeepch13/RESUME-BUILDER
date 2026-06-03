package com.atsforge.platform.export;

import com.atsforge.platform.resume.ResumeEntity;
import com.atsforge.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "export_jobs")
public class ExportJobEntity {
    public enum Status { QUEUED, COMPLETED, FAILED }
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id")
    private ResumeEntity resume;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;
    @Column(nullable = false, length = 20)
    private String format;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.QUEUED;
    @Column(name = "object_key", length = 500)
    private String objectKey;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected ExportJobEntity() {}
    public ExportJobEntity(ResumeEntity resume, UserEntity user, String format) {
        this.resume = resume; this.requestedBy = user; this.format = format;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public void complete(String objectKey) { status = Status.COMPLETED; this.objectKey = objectKey; completedAt = Instant.now(); }
    public void fail(String reason) { status = Status.FAILED; errorMessage = reason; completedAt = Instant.now(); }
}
