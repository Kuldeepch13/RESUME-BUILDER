package com.atsforge.platform.export;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.UUID;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.atsforge.platform.common.ForbiddenException;
import com.atsforge.platform.config.AppProperties;
import com.atsforge.platform.resume.ResumeEntity;
import com.atsforge.platform.resume.ResumeService;
import com.atsforge.platform.security.SecurityUtils;
import com.atsforge.platform.storage.ObjectStorageService;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import com.atsforge.platform.user.UserRole;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

@Service
public class ExportService {
    public enum Format {
        PDF("application/pdf", "pdf"), DOCX("application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx"),
        TXT("text/plain", "txt"), HTML("text/html", "html"), JSON("application/json", "json");
        public final String contentType;
        public final String extension;
        Format(String contentType, String extension) { this.contentType = contentType; this.extension = extension; }
    }
    private final ResumeService resumes;
    private final ResumeRenderer renderer;
    private final ExportJobRepository jobs;
    private final UserRepository users;
    private final ObjectStorageService storage;
    private final ObjectMapper mapper;
    private final AppProperties properties;
    private final ExportJobProcessor processor;

    public ExportService(ResumeService resumes, ResumeRenderer renderer, ExportJobRepository jobs, UserRepository users,
                         ObjectStorageService storage, ObjectMapper mapper, AppProperties properties, ExportJobProcessor processor) {
        this.resumes = resumes; this.renderer = renderer; this.jobs = jobs; this.users = users; this.storage = storage; this.mapper = mapper;
        this.properties = properties; this.processor = processor;
    }

    @Transactional
    public ExportedFile export(UUID resumeId, String requestedFormat) {
        UUID userId = SecurityUtils.currentUserId();
        UserEntity user = users.findById(userId).orElseThrow();
        if (user.getRole() == UserRole.USER
                && jobs.countByRequestedByIdAndCreatedAtAfter(userId, Instant.now().minus(31, ChronoUnit.DAYS))
                >= properties.limits().freeExportsPerMonth()) {
            throw new ForbiddenException("Free export allowance reached. Upgrade to continue exporting.");
        }
        ResumeEntity resume = resumes.ownedEntity(resumeId);
        Format format = Format.valueOf(requestedFormat.toUpperCase(Locale.ROOT));
        ExportJobEntity job = jobs.save(new ExportJobEntity(resume, user, format.name()));
        byte[] bytes = render(resume, format);
        
        // Queue async job for storage, but return file immediately for synchronous export
        processor.process(job.getId(), bytes, userId.toString(), resumeId.toString(), format.name());
        resume.exported();
        return new ExportedFile(bytes, format.contentType, safeName(resume.getTitle()) + "." + format.extension);
    }

    private byte[] render(ResumeEntity resume, Format format) {
        try {
            return switch (format) {
                case HTML -> renderer.html(resume).getBytes(StandardCharsets.UTF_8);
                case TXT -> renderer.text(resume).getBytes(StandardCharsets.UTF_8);
                case JSON -> mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(mapper.readTree(resume.getSectionsJson()));
                case PDF -> pdf(renderer.html(resume));
                case DOCX -> docx(resume);
            };
        } catch (Exception ex) {
            throw new IllegalStateException("Resume export failed.", ex);
        }
    }

    private byte[] pdf(String html) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new PdfRendererBuilder().withHtmlContent(html, null).toStream(output).run();
        return output.toByteArray();
    }

    private byte[] docx(ResumeEntity resume) throws Exception {
        try (XWPFDocument document = new XWPFDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (String line : renderer.text(resume).split("\\n")) {
                XWPFParagraph paragraph = document.createParagraph();
                paragraph.createRun().setText(line);
            }
            document.write(output);
            return output.toByteArray();
        }
    }

    private String safeName(String title) { return title.replaceAll("[^A-Za-z0-9 _-]", "").trim().replace(" ", "-"); }
    public record ExportedFile(byte[] bytes, String contentType, String filename) {}
}
