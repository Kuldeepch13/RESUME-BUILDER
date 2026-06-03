package com.atsforge.platform.billing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "payment_events")
public class PaymentEventEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Column(name = "stripe_event_id", nullable = false, unique = true, length = 120)
    private String stripeEventId;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_json", nullable = false, columnDefinition = "jsonb")
    private String payloadJson;
    @Column(name = "processing_status", nullable = false, length = 30)
    private String processingStatus = "RECEIVED";
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    protected PaymentEventEntity() {}
    public PaymentEventEntity(String eventId, String eventType, String payloadJson) {
        this.stripeEventId = eventId; this.eventType = eventType; this.payloadJson = payloadJson;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public void processed() { processingStatus = "PROCESSED"; processedAt = Instant.now(); }
}
