package com.atsforge.platform.billing;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/billing")
public class BillingController {
    private final BillingService service;
    public BillingController(BillingService service) { this.service = service; }
    @PostMapping("/checkout")
    public BillingService.CheckoutResponse checkout(@RequestParam(defaultValue = "month") String interval) {
        return service.checkout(interval);
    }
    @GetMapping("/subscription")
    public BillingService.SubscriptionResponse subscription() { return service.current(); }
}

