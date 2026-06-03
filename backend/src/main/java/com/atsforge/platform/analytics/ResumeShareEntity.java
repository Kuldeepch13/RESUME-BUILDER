package com.atsforge.platform.analytics;

import com.atsforge.platform.resume.ResumeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "resume_shares")
public class ResumeShareEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id")
    private ResumeEntity resume;
    @Column(name = "public_token", nullable = false, unique = true, length = 80)
    private String publicToken;
    @Column(nullable = false)
    private boolean active = true;
    @Column(name = "expires_at")
    private Instant expiresAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResumeShareEntity() {}
    public ResumeShareEntity(ResumeEntity resume, String token, Instant expiresAt) {
        this.resume = resume; this.publicToken = token; this.expiresAt = expiresAt;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public ResumeEntity getResume() { return resume; }
    public String getPublicToken() { return publicToken; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean usable() { return active && (expiresAt == null || expiresAt.isAfter(Instant.now())); }
    public void disable() { active = false; }
}
