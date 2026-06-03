package com.atsforge.platform.billing;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<SubscriptionEntity, UUID> {
    Optional<SubscriptionEntity> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);
    Optional<SubscriptionEntity> findByStripeSubscriptionId(String subscriptionId);
    long countByPlanAndStatus(SubscriptionEntity.Plan plan, SubscriptionEntity.Status status);
}

