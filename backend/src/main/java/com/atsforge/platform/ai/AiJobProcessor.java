package com.atsforge.platform.ai;

import com.atsforge.platform.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiJobProcessor {
    private final AiAnalysisRepository analyses;
    private final OpenAiClient openAi;
    private final LocalAtsAnalyzer fallback;
    private final ObjectMapper mapper;
    private final AppProperties properties;

    public AiJobProcessor(AiAnalysisRepository analyses, OpenAiClient openAi, LocalAtsAnalyzer fallback,
                          ObjectMapper mapper, AppProperties properties) {
        this.analyses = analyses;
        this.openAi = openAi;
        this.fallback = fallback;
        this.mapper = mapper;
        this.properties = properties;
    }

    @Async
    @Transactional
    public void process(UUID analysisId) {
        AiAnalysisEntity analysis = analyses.findById(analysisId).orElseThrow();
        analysis.processing();
        try {
            String input = "RESUME_JSON:\n" + analysis.getResume().getSectionsJson() + "\nJOB_DESCRIPTION:\n"
                    + (analysis.getJobDescription() == null ? "Not provided" : analysis.getJobDescription());
            Optional<OpenAiClient.Completion> completion = openAi.structured(
                    "You are a precise ATS resume reviewer. Score fairly and return concise, actionable feedback.", input,
                    "resume_analysis", OpenAiClient.analysisSchema());
            JsonNode result = completion.map(OpenAiClient.Completion::payload)
                    .orElseGet(() -> fallback.analyze(analysis.getResume().getSectionsJson(), analysis.getJobDescription()));
            analysis.complete(result.path("atsScore").asInt(), result.path("matchPercentage").asInt(),
                    mapper.writeValueAsString(result), completion.map(OpenAiClient.Completion::model).orElse("local-ats-v1"),
                    completion.map(OpenAiClient.Completion::inputTokens).orElse(null),
                    completion.map(OpenAiClient.Completion::outputTokens).orElse(null));
        } catch (Exception ex) {
            analysis.fail(ex.getMessage() == null ? "Analysis failed." : ex.getMessage());
        }
    }
}

