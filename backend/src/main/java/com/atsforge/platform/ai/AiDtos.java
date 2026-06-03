package com.atsforge.platform.ai;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AiDtos {
    private AiDtos() {}
    public record AnalyzeRequest(@Size(max = 20000) String jobDescription) {}
    public record AnalysisResponse(UUID id, String status, Integer atsScore, Integer matchPercentage, JsonNode result,
                                   String model, Instant createdAt) {}
    public record GenerateRequest(@NotBlank @Size(max = 4000) String prompt, @Size(max = 20000) String jobDescription) {}
    public record GenerationResponse(JsonNode sections, String model, boolean generatedByAi) {}
    public record BulletPointRequest(@NotBlank @Size(max = 4000) String context, @NotBlank @Size(max = 500) String title) {}
    public record BulletPointResponse(String[] bullets, String model, boolean generatedByAi) {}
    public record ImprovementRequest(@NotBlank @Size(max = 20000) String jobDescription) {}
    public record ImprovementResponse(JsonNode suggestions, String model, boolean generatedByAi) {}
}

