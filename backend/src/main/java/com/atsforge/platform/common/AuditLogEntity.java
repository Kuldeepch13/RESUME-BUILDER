package com.atsforge.platform.common;

import com.atsforge.platform.user.UserEntity;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "audit_logs")
public class AuditLogEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "actor_id")
    private UserEntity actor;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;
    @Column(name = "entity_id")
    private UUID entityId;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metadata_json", nullable = false, columnDefinition = "jsonb")
    private String metadataJson = "{}";
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected AuditLogEntity() {}
    public AuditLogEntity(UserEntity actor, String action, String entityType, UUID entityId, String metadataJson) {
        this.actor = actor; this.action = action; this.entityType = entityType; this.entityId = entityId; this.metadataJson = metadataJson;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public String getAction() { return action; }
    public String getEntityType() { return entityType; }
    public UUID getEntityId() { return entityId; }
    public Instant getCreatedAt() { return createdAt; }
}
