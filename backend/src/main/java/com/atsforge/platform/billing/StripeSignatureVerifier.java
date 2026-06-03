package com.atsforge.platform.billing;

import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class StripeSignatureVerifier {
    private final AppProperties properties;
    public StripeSignatureVerifier(AppProperties properties) { this.properties = properties; }

    public void verify(String payload, String signature) {
        if (properties.stripe().webhookSecret().isBlank() || signature == null) {
            throw new ForbiddenException("Stripe webhook verification is not configured.");
        }
        String timestamp = null;
        String expected = null;
        for (String part : signature.split(",")) {
            if (part.startsWith("t=")) timestamp = part.substring(2);
            if (part.startsWith("v1=")) expected = part.substring(3);
        }
        if (timestamp == null || expected == null || Math.abs(Instant.now().getEpochSecond() - Long.parseLong(timestamp)) > 300) {
            throw new ForbiddenException("Invalid webhook signature.");
        }
        try {
            Mac hmac = Mac.getInstance("HmacSHA256");
            hmac.init(new SecretKeySpec(properties.stripe().webhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String actual = HexFormat.of().formatHex(hmac.doFinal((timestamp + "." + payload).getBytes(StandardCharsets.UTF_8)));
            if (!MessageDigest.isEqual(actual.getBytes(StandardCharsets.UTF_8), expected.getBytes(StandardCharsets.UTF_8))) {
                throw new ForbiddenException("Invalid webhook signature.");
            }
        } catch (ForbiddenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to verify webhook.", ex);
        }
    }
}

