package com.atsforge.platform.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.atsforge.platform.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class OpenAiClient {
    private final AppProperties properties;
    private final RestClient client;
    private final ObjectMapper mapper;

    public OpenAiClient(AppProperties properties, RestClient.Builder builder, ObjectMapper mapper) {
        this.properties = properties;
        this.mapper = mapper;
        this.client = builder.baseUrl(properties.openai().baseUrl().toString()).build();
    }

    public Optional<Completion> structured(String instructions, String input, String schemaName, Map<String, Object> schema) {
        if (properties.openai().apiKey() == null || properties.openai().apiKey().isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> request = Map.of(
                "model", properties.openai().model(),
                "instructions", instructions,
                "input", input,
                "text", Map.of("format", Map.of("type", "json_schema", "name", schemaName, "strict", true, "schema", schema)));
        JsonNode result = client.post().uri("/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.openai().apiKey())
                .body(request).retrieve().body(JsonNode.class);
        JsonNode outputText = result.path("output").path(0).path("content").path(0).path("text");
        JsonNode payload = read(outputText.asText());
        JsonNode usage = result.path("usage");
        return Optional.of(new Completion(payload, properties.openai().model(),
                usage.path("input_tokens").isInt() ? usage.path("input_tokens").asInt() : null,
                usage.path("output_tokens").isInt() ? usage.path("output_tokens").asInt() : null));
    }

    private JsonNode read(String text) {
        try { return mapper.readTree(text); }
        catch (Exception ex) { throw new IllegalStateException("AI response was not valid JSON.", ex); }
    }

    public record Completion(JsonNode payload, String model, Integer inputTokens, Integer outputTokens) {}

    public static Map<String, Object> analysisSchema() {
        return Map.of(
                "type", "object",
                "additionalProperties", false,
                "required", List.of("atsScore", "matchPercentage", "missingKeywords", "suggestions", "strongSections", "weakSections"),
                "properties", Map.of(
                        "atsScore", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "matchPercentage", Map.of("type", "integer", "minimum", 0, "maximum", 100),
                        "missingKeywords", Map.of("type", "array", "items", Map.of("type", "string")),
                        "suggestions", Map.of("type", "array", "items", Map.of("type", "string")),
                        "strongSections", Map.of("type", "array", "items", Map.of("type", "string")),
                        "weakSections", Map.of("type", "array", "items", Map.of("type", "string"))));
    }

    public static Map<String, Object> generationSchema() {
        return Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("sections"),
                "properties", Map.of("sections", Map.of("type", "array", "items", Map.of(
                        "type", "object", "additionalProperties", false,
                        "required", List.of("type", "title", "content"),
                        "properties", Map.of(
                                "type", Map.of("type", "string"),
                                "title", Map.of("type", "string"),
                                "content", Map.of("type", "string"))))));
    }

    public static Map<String, Object> bulletSchema() {
        return Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("bullets"),
                "properties", Map.of("bullets", Map.of("type", "array", "maxItems", 5, "items", Map.of("type", "string"))));
    }

    public static Map<String, Object> suggestionsSchema() {
        return Map.of(
                "type", "object", "additionalProperties", false, "required", List.of("suggestions"),
                "properties", Map.of("suggestions", Map.of("type", "array", "items", Map.of(
                        "type", "object", "additionalProperties", false,
                        "required", List.of("category", "issue", "improvement"),
                        "properties", Map.of(
                                "category", Map.of("type", "string", "enum", List.of("KEYWORDS", "FORMATTING", "CLARITY", "METRICS", "EXPERIENCE")),
                                "issue", Map.of("type", "string"),
                                "improvement", Map.of("type", "string"))))));
    }
}
