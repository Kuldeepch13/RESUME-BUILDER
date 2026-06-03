package com.atsforge.platform.billing;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks")
public class StripeWebhookController {
    private final BillingService service;
    public StripeWebhookController(BillingService service) { this.service = service; }
    @PostMapping("/stripe") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void stripe(@RequestHeader("Stripe-Signature") String signature, @RequestBody String payload) {
        service.webhook(signature, payload);
    }
}
