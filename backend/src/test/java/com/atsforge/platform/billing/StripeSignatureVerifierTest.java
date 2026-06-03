package com.atsforge.platform.billing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class StripeSignatureVerifierTest {
    @Test
    void rejectsUnsignedWebhookPayloads() {
        AppProperties properties = new AppProperties(URI.create("https://app.example.com"), "https://app.example.com",
                new AppProperties.Jwt("issuer", "a-secret-value-long-enough-to-sign-json-web-tokens-123456789", Duration.ofMinutes(15), Duration.ofDays(30)),
                new AppProperties.OpenAi("", "gpt", URI.create("https://api.openai.com/v1")),
                new AppProperties.Stripe("", "whsec_test", "monthly", "yearly"),
                new AppProperties.Storage(URI.create("https://storage.example.com"), "us-east-1", "bucket", "key", "secret"),
                new AppProperties.Limits(100, 3, 5));

        StripeSignatureVerifier verifier = new StripeSignatureVerifier(properties);

        assertThatThrownBy(() -> verifier.verify("{}", null)).isInstanceOf(ForbiddenException.class);
    }
}
