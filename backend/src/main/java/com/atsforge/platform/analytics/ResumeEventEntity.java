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
@Table(name = "resume_events")
public class ResumeEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id")
    private ResumeEntity resume;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "share_id")
    private ResumeShareEntity share;
    @Column(name = "event_type", nullable = false, length = 30)
    private String eventType;
    @Column(name = "visitor_hash", length = 64)
    private String visitorHash;
    @Column(length = 500)
    private String referrer;
    @Column(name = "user_agent", length = 300)
    private String userAgent;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected ResumeEventEntity() {}
    public ResumeEventEntity(ResumeShareEntity share, String type, String visitorHash, String referrer, String userAgent) {
        this.resume = share.getResume(); this.share = share; this.eventType = type; this.visitorHash = visitorHash;
        this.referrer = referrer; this.userAgent = userAgent;
    }
    @PrePersist void create() { occurredAt = Instant.now(); }
}
