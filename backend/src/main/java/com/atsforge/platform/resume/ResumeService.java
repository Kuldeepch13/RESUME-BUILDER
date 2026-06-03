package com.atsforge.platform.resume;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atsforge.platform.common.AuditService;
import com.atsforge.platform.common.NotFoundException;
import com.atsforge.platform.resume.ResumeDtos.CreateResumeRequest;
import com.atsforge.platform.resume.ResumeDtos.ResumeResponse;
import com.atsforge.platform.resume.ResumeDtos.UpdateResumeRequest;
import com.atsforge.platform.resume.ResumeDtos.VersionResponse;
import com.atsforge.platform.security.SecurityUtils;
import com.atsforge.platform.template.TemplateEntity;
import com.atsforge.platform.template.TemplateRepository;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class ResumeService {
    private static final String STARTER_SECTIONS = """
            [{"id":"personal","type":"PERSONAL","title":"Personal information","order":0,"visible":true,"content":{}},
             {"id":"summary","type":"SUMMARY","title":"Professional summary","order":1,"visible":true,"content":{}},
             {"id":"experience","type":"EXPERIENCE","title":"Experience","order":2,"visible":true,"content":[]},
             {"id":"education","type":"EDUCATION","title":"Education","order":3,"visible":true,"content":[]},
             {"id":"skills","type":"SKILLS","title":"Skills","order":4,"visible":true,"content":[]}]""";
    private static final String DEFAULT_THEME = "{\"primaryColor\":\"#115e59\",\"fontFamily\":\"Inter\",\"fontSize\":10}";
    private final ResumeRepository resumes;
    private final ResumeVersionRepository versions;
    private final TemplateRepository templates;
    private final UserRepository users;
    private final ObjectMapper mapper;
    private final SimpMessagingTemplate messaging;
    private final AuditService audit;

    public ResumeService(ResumeRepository resumes, ResumeVersionRepository versions, TemplateRepository templates,
                         UserRepository users, ObjectMapper mapper, SimpMessagingTemplate messaging, AuditService audit) {
        this.resumes = resumes;
        this.versions = versions;
        this.templates = templates;
        this.users = users;
        this.mapper = mapper;
        this.messaging = messaging;
        this.audit = audit;
    }

    @Transactional
    public ResumeResponse create(CreateResumeRequest request) {
        UserEntity owner = users.getReferenceById(SecurityUtils.currentUserId());
        TemplateEntity template = template(request.templateCode() == null ? "atlas" : request.templateCode());
        String slug = slugify(request.title()) + "-" + UUID.randomUUID().toString().substring(0, 8);
        ResumeEntity resume = resumes.save(new ResumeEntity(owner, template, request.title().trim(), slug, STARTER_SECTIONS, DEFAULT_THEME));
        resume.edit(resume.getTitle(), request.targetRole(), template, STARTER_SECTIONS, DEFAULT_THEME);
        versions.save(new ResumeVersionEntity(resume, owner, "Resume created"));
        audit.record("RESUME_CREATED", "RESUME", resume.getId());
        return response(resume);
    }

    @Transactional(readOnly = true)
    public Page<ResumeResponse> list(Pageable pageable) {
        return resumes.findAllByOwnerId(SecurityUtils.currentUserId(), pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ResumeResponse> search(String query, Pageable pageable) {
        return resumes.searchByTitleOrRole(SecurityUtils.currentUserId(), query, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ResumeResponse> filterByStatus(String status, Pageable pageable) {
        return resumes.findAllByOwnerIdAndStatus(SecurityUtils.currentUserId(), 
                ResumeEntity.Status.valueOf(status.toUpperCase(java.util.Locale.ROOT)), pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public Page<ResumeResponse> filterByTemplate(UUID templateId, Pageable pageable) {
        return resumes.findAllByOwnerIdAndTemplateId(SecurityUtils.currentUserId(), templateId, pageable).map(this::response);
    }

    @Transactional(readOnly = true)
    public ResumeResponse get(UUID id) {
        return response(owned(id));
    }

    @Transactional
    public ResumeResponse autosave(UUID id, UpdateResumeRequest request) {
        ResumeEntity resume = owned(id);
        resume.edit(request.title().trim(), request.targetRole(), template(request.templateCode()),
                json(request.sections()), json(request.theme()));
        messaging.convertAndSend("/topic/resumes/" + id, java.util.Map.of("event", "SAVED", "resumeId", id));
        return response(resume);
    }

    @Transactional
    public ResumeResponse snapshot(UUID id, UpdateResumeRequest request) {
        ResumeEntity resume = owned(id);
        resume.edit(request.title().trim(), request.targetRole(), template(request.templateCode()),
                json(request.sections()), json(request.theme()));
        resume.incrementVersion();
        versions.save(new ResumeVersionEntity(resume, resume.getOwner(),
                request.changeSummary() == null ? "Manual save" : request.changeSummary()));
        audit.record("RESUME_VERSION_CREATED", "RESUME", resume.getId());
        return response(resume);
    }

    @Transactional
    public ResumeResponse duplicate(UUID id) {
        ResumeEntity source = owned(id);
        UserEntity owner = source.getOwner();
        ResumeEntity copy = resumes.save(new ResumeEntity(owner, source.getTemplate(), source.getTitle() + " (Copy)",
                slugify(source.getTitle()) + "-copy-" + UUID.randomUUID().toString().substring(0, 8),
                source.getSectionsJson(), source.getThemeJson()));
        versions.save(new ResumeVersionEntity(copy, owner, "Duplicated from " + source.getTitle()));
        audit.record("RESUME_DUPLICATED", "RESUME", copy.getId());
        return response(copy);
    }

    @Transactional(readOnly = true)
    public List<VersionResponse> versions(UUID id) {
        owned(id);
        return versions.findAllByResumeIdOrderByVersionNumberDesc(id).stream()
                .map(v -> new VersionResponse(v.getVersionNumber(), tree(v.getSectionsJson()), tree(v.getThemeJson()),
                        v.getChangeSummary(), v.getCreatedAt())).toList();
    }

    @Transactional
    public void delete(UUID id) {
        ResumeEntity resume = owned(id);
        resumes.delete(resume);
        audit.record("RESUME_DELETED", "RESUME", id);
    }

    public ResumeEntity ownedEntity(UUID id) { return owned(id); }

    private ResumeEntity owned(UUID id) {
        return resumes.findByIdAndOwnerId(id, SecurityUtils.currentUserId())
                .orElseThrow(() -> new NotFoundException("Resume not found."));
    }
    private TemplateEntity template(String code) {
        return templates.findByCodeAndActiveTrue(code == null ? "atlas" : code)
                .orElseThrow(() -> new NotFoundException("Template not found."));
    }
    private ResumeResponse response(ResumeEntity r) {
        return new ResumeResponse(r.getId(), r.getTitle(), r.getSlug(), r.getStatus().name(), r.getTargetRole(),
                r.getTemplate() == null ? null : r.getTemplate().getCode(), tree(r.getSectionsJson()), tree(r.getThemeJson()),
                r.getVersionNumber(), r.getUpdatedAt());
    }
    private String json(JsonNode node) {
        try { return mapper.writeValueAsString(node); }
        catch (JsonProcessingException ex) { throw new IllegalArgumentException("Invalid resume JSON.", ex); }
    }
    private JsonNode tree(String value) {
        try { return mapper.readTree(value); }
        catch (JsonProcessingException ex) { throw new IllegalStateException("Stored resume JSON is invalid.", ex); }
    }
    private String slugify(String title) {
        String slug = title.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return slug.isBlank() ? "resume" : slug;
    }
}
