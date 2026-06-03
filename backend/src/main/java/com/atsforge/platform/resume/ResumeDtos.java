package com.atsforge.platform.resume;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.UUID;

public final class ResumeDtos {
    private ResumeDtos() {}
    public record CreateResumeRequest(@NotBlank @Size(max = 160) String title, @Size(max = 80) String templateCode,
                                      @Size(max = 120) String targetRole) {}
    public record UpdateResumeRequest(@NotBlank @Size(max = 160) String title, @Size(max = 80) String templateCode,
                                      @Size(max = 120) String targetRole, @NotNull JsonNode sections,
                                      @NotNull JsonNode theme, @Size(max = 240) String changeSummary) {}
    public record ResumeResponse(UUID id, String title, String slug, String status, String targetRole, String templateCode,
                                 JsonNode sections, JsonNode theme, int versionNumber, Instant updatedAt) {}
    public record VersionResponse(int versionNumber, JsonNode sections, JsonNode theme, String changeSummary, Instant createdAt) {}
}

