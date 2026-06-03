package com.atsforge.platform.billing;

import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.security.SecurityUtils;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import com.atsforge.platform.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Service
public class BillingService {
    private final SubscriptionRepository subscriptions;
    private final PaymentEventRepository paymentEvents;
    private final UserRepository users;
    private final AppProperties properties;
    private final StripeSignatureVerifier verifier;
    private final ObjectMapper mapper;
    private final RestClient stripe;

    public BillingService(SubscriptionRepository subscriptions, PaymentEventRepository paymentEvents, UserRepository users,
                          AppProperties properties, StripeSignatureVerifier verifier, ObjectMapper mapper,
                          RestClient.Builder builder) {
        this.subscriptions = subscriptions; this.paymentEvents = paymentEvents; this.users = users;
        this.properties = properties; this.verifier = verifier; this.mapper = mapper;
        this.stripe = builder.baseUrl("https://api.stripe.com/v1").build();
    }

    public CheckoutResponse checkout(String interval) {
        if (properties.stripe().secretKey().isBlank()) throw new ForbiddenException("Billing is not configured.");
        UserEntity user = users.findById(SecurityUtils.currentUserId()).orElseThrow();
        String price = "year".equalsIgnoreCase(interval) ? properties.stripe().proYearlyPriceId() : properties.stripe().proMonthlyPriceId();
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("mode", "subscription");
        form.add("line_items[0][price]", price);
        form.add("line_items[0][quantity]", "1");
        form.add("client_reference_id", user.getId().toString());
        form.add("customer_email", user.getEmail());
        form.add("success_url", properties.publicUrl() + "/billing/success?session_id={CHECKOUT_SESSION_ID}");
        form.add("cancel_url", properties.publicUrl() + "/settings/billing");
        JsonNode result = stripe.post().uri("/checkout/sessions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.stripe().secretKey())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(JsonNode.class);
        return new CheckoutResponse(result.path("url").asText());
    }

    @Transactional(readOnly = true)
    public SubscriptionResponse current() {
        return subscriptions.findFirstByUserIdOrderByCreatedAtDesc(SecurityUtils.currentUserId())
                .map(s -> new SubscriptionResponse(s.getPlan().name(), s.getStatus().name(), s.getCurrentPeriodEnd()))
                .orElse(new SubscriptionResponse("FREE", "ACTIVE", null));
    }

    @Transactional
    public void webhook(String signature, String payload) {
        verifier.verify(payload, signature);
        try {
            JsonNode event = mapper.readTree(payload);
            String eventId = event.path("id").asText();
            if (paymentEvents.existsByStripeEventId(eventId)) return;
            PaymentEventEntity stored = paymentEvents.save(new PaymentEventEntity(eventId, event.path("type").asText(), payload));
            JsonNode object = event.path("data").path("object");
            switch (event.path("type").asText()) {
                case "checkout.session.completed" -> startSubscription(object);
                case "customer.subscription.updated" -> updateSubscription(object);
                case "customer.subscription.deleted" -> cancelSubscription(object.path("id").asText());
                default -> { }
            }
            stored.processed();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid Stripe event payload.", ex);
        }
    }

    private void startSubscription(JsonNode session) {
        UUID userId = UUID.fromString(session.path("client_reference_id").asText());
        UserEntity user = users.findById(userId).orElseThrow();
        SubscriptionEntity subscription = subscriptions.findFirstByUserIdOrderByCreatedAtDesc(userId)
                .orElseGet(() -> new SubscriptionEntity(user));
        subscription.activatePro(session.path("customer").asText(), session.path("subscription").asText(), null, false);
        user.setRole(UserRole.PREMIUM_USER);
        subscriptions.save(subscription);
    }
    private void updateSubscription(JsonNode object) {
        subscriptions.findByStripeSubscriptionId(object.path("id").asText()).ifPresent(subscription ->
                subscription.activatePro(object.path("customer").asText(), object.path("id").asText(),
                        Instant.ofEpochSecond(object.path("current_period_end").asLong()), object.path("cancel_at_period_end").asBoolean()));
    }
    private void cancelSubscription(String id) {
        subscriptions.findByStripeSubscriptionId(id).ifPresent(subscription -> {
            subscription.cancel();
            subscription.getUser().setRole(UserRole.USER);
        });
    }
    public record CheckoutResponse(String checkoutUrl) {}
    public record SubscriptionResponse(String plan, String status, Instant periodEnd) {}
}

