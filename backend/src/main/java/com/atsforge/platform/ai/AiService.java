package com.atsforge.platform.ai;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.atsforge.platform.ai.AiDtos.AnalysisResponse;
import com.atsforge.platform.ai.AiDtos.AnalyzeRequest;
import com.atsforge.platform.ai.AiDtos.BulletPointRequest;
import com.atsforge.platform.ai.AiDtos.BulletPointResponse;
import com.atsforge.platform.ai.AiDtos.GenerateRequest;
import com.atsforge.platform.ai.AiDtos.GenerationResponse;
import com.atsforge.platform.ai.AiDtos.ImprovementRequest;
import com.atsforge.platform.ai.AiDtos.ImprovementResponse;
import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.resume.ResumeEntity;
import com.atsforge.platform.resume.ResumeService;
import com.atsforge.platform.security.SecurityUtils;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import com.atsforge.platform.user.UserRole;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Service
public class AiService {
    private final AiAnalysisRepository analyses;
    private final ResumeService resumes;
    private final UserRepository users;
    private final AiJobProcessor processor;
    private final OpenAiClient openAi;
    private final LocalAtsAnalyzer fallback;
    private final ObjectMapper mapper;
    private final AppProperties properties;

    public AiService(AiAnalysisRepository analyses, ResumeService resumes, UserRepository users, AiJobProcessor processor,
                     OpenAiClient openAi, LocalAtsAnalyzer fallback, ObjectMapper mapper, AppProperties properties) {
        this.analyses = analyses; this.resumes = resumes; this.users = users; this.processor = processor;
        this.openAi = openAi; this.fallback = fallback; this.mapper = mapper; this.properties = properties;
    }

    @Transactional
    public AnalysisResponse request(UUID resumeId, AnalyzeRequest request) {
        UUID userId = SecurityUtils.currentUserId();
        UserEntity user = users.findById(userId).orElseThrow();
        if (user.getRole() == UserRole.USER
                && analyses.countByRequestedByIdAndCreatedAtAfter(userId, Instant.now().minus(1, ChronoUnit.HOURS))
                >= properties.limits().aiPerHourFree()) {
            throw new ForbiddenException("AI hourly allowance reached. Upgrade or try again later.");
        }
        ResumeEntity resume = resumes.ownedEntity(resumeId);
        AiAnalysisEntity analysis = analyses.save(new AiAnalysisEntity(resume, user, request.jobDescription()));
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                processor.process(analysis.getId());
            }
        });
        return response(analysis);
    }

    @Transactional(readOnly = true)
    public AnalysisResponse get(UUID id) {
        return analyses.findByIdAndRequestedById(id, SecurityUtils.currentUserId()).map(this::response)
                .orElseThrow(() -> new com.atsforge.platform.common.NotFoundException("Analysis not found."));
    }

    public GenerationResponse generate(GenerateRequest request) {
        Optional<OpenAiClient.Completion> completion = openAi.structured(
                "Produce truthful ATS-friendly resume sections. Do not invent employers or credentials; use placeholders for missing facts.",
                request.prompt() + "\nTARGET_JOB:\n" + (request.jobDescription() == null ? "" : request.jobDescription()),
                "resume_generation", OpenAiClient.generationSchema());
        JsonNode sections = completion.map(c -> c.payload().path("sections")).orElseGet(() -> outline(request.prompt()));
        return new GenerationResponse(sections, completion.map(OpenAiClient.Completion::model).orElse("local-outline-v1"), completion.isPresent());
    }

    public BulletPointResponse generateBulletPoints(UUID resumeId, BulletPointRequest request) {
        resumes.ownedEntity(resumeId); // Verify ownership
        Optional<OpenAiClient.Completion> completion = openAi.structured(
                "Generate 3-5 concrete, measurable bullet points for a resume.",
                "CONTEXT: " + request.context() + "\nTITLE: " + request.title(),
                "bullet_point_generation", OpenAiClient.bulletSchema());
        String[] bullets = completion.map(c -> c.payload().path("bullets").toString().split("\n"))
                .orElse(new String[]{"Added responsibility in " + request.title(), "Contributed to team initiatives"});
        return new BulletPointResponse(bullets, completion.map(OpenAiClient.Completion::model).orElse("local-bullets-v1"), completion.isPresent());
    }

    public ImprovementResponse getImprovementSuggestions(UUID resumeId, ImprovementRequest request) {
        resumes.ownedEntity(resumeId); // Verify ownership
        Optional<OpenAiClient.Completion> completion = openAi.structured(
                "Provide specific, actionable ATS and hiring improvements for this resume.",
                "RESUME_JSON: " + resumes.ownedEntity(resumeId).getSectionsJson() + "\nJOB_DESCRIPTION: " + request.jobDescription(),
                "improvement_suggestions", OpenAiClient.suggestionsSchema());
        JsonNode suggestions = completion.map(OpenAiClient.Completion::payload)
                .orElseGet(() -> mapper.createObjectNode().put("suggestions", "Add specific metrics and keywords matching the job description"));
        return new ImprovementResponse(suggestions, completion.map(OpenAiClient.Completion::model).orElse("local-suggest-v1"), completion.isPresent());
    }

    private ArrayNode outline(String prompt) {
        ArrayNode sections = mapper.createArrayNode();
        sections.add(mapper.createObjectNode().put("type", "SUMMARY").put("title", "Professional Summary")
                .put("content", "Draft a focused summary for: " + prompt));
        sections.add(mapper.createObjectNode().put("type", "EXPERIENCE").put("title", "Experience")
                .put("content", "Add verified employers, responsibilities and measurable outcomes."));
        sections.add(mapper.createObjectNode().put("type", "SKILLS").put("title", "Skills")
                .put("content", "Add skills you can demonstrate in interviews."));
        return sections;
    }

    private AnalysisResponse response(AiAnalysisEntity entity) {
        JsonNode result = null;
        if (entity.getResultJson() != null) {
            try { result = mapper.readTree(entity.getResultJson()); }
            catch (Exception ignored) { result = mapper.createObjectNode(); }
        }
        return new AnalysisResponse(entity.getId(), entity.getStatus().name(), entity.getAtsScore(),
                entity.getMatchPercentage(), result, entity.getModel(), entity.getCreatedAt());
    }
}
