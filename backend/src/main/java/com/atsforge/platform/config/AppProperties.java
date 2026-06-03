package com.atsforge.platform.config;

import java.net.URI;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        URI publicUrl,
        String corsOrigins,
        Jwt jwt,
        OpenAi openai,
        Stripe stripe,
        Storage storage,
        Limits limits) {
    public record Jwt(String issuer, String secret, Duration accessTokenDuration, Duration refreshTokenDuration) {}
    public record OpenAi(String apiKey, String model, URI baseUrl) {}
    public record Stripe(String secretKey, String webhookSecret, String proMonthlyPriceId, String proYearlyPriceId) {}
    public record Storage(URI endpoint, String region, String bucket, String accessKey, String secretKey) {}
    public record Limits(int apiPerMinute, int aiPerHourFree, int freeExportsPerMonth) {}
}

