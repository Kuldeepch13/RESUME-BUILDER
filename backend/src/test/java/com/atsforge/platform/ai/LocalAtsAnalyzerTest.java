package com.atsforge.platform.ai;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class LocalAtsAnalyzerTest {
    private final LocalAtsAnalyzer analyzer = new LocalAtsAnalyzer(new ObjectMapper());

    @Test
    void identifiesMissingJobKeywordsAndReturnsBoundedScores() {
        JsonNode result = analyzer.analyze(
                "[{\"title\":\"Skills\",\"content\":\"Java Spring PostgreSQL\"}]",
                "Java Spring Kubernetes observability");

        assertThat(result.path("atsScore").asInt()).isBetween(0, 100);
        assertThat(result.path("matchPercentage").asInt()).isBetween(1, 99);
        assertThat(result.path("missingKeywords").toString()).contains("kubernetes", "observability");
        assertThat(result.path("suggestions").size()).isGreaterThan(0);
    }
}
