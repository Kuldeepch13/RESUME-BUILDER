package com.atsforge.platform.billing;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "subscriptions")
public class SubscriptionEntity {
    public enum Plan { FREE, PRO, ENTERPRISE }
    public enum Status { TRIALING, ACTIVE, PAST_DUE, CANCELED, INCOMPLETE }
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "user_id")
    private UserEntity user;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Plan plan;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status;
    @Column(name = "stripe_customer_id", length = 100)
    private String stripeCustomerId;
    @Column(name = "stripe_subscription_id", length = 100)
    private String stripeSubscriptionId;
    @Column(name = "current_period_end")
    private Instant currentPeriodEnd;
    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected SubscriptionEntity() {}
    public SubscriptionEntity(UserEntity user) { this.user = user; this.plan = Plan.FREE; this.status = Status.ACTIVE; }
    @PrePersist void create() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updateTime() { updatedAt = Instant.now(); }
    public UserEntity getUser() { return user; }
    public Plan getPlan() { return plan; }
    public Status getStatus() { return status; }
    public Instant getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void activatePro(String customerId, String subscriptionId, Instant periodEnd, boolean cancel) {
        plan = Plan.PRO; status = Status.ACTIVE; stripeCustomerId = customerId; stripeSubscriptionId = subscriptionId;
        currentPeriodEnd = periodEnd; cancelAtPeriodEnd = cancel;
    }
    public void cancel() { status = Status.CANCELED; plan = Plan.FREE; }
}
