package com.atsforge.platform.resume;

import com.atsforge.platform.template.TemplateEntity;
import com.atsforge.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resumes")
public class ResumeEntity {
    public enum Status { DRAFT, PUBLISHED, ARCHIVED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "owner_id")
    private UserEntity owner;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private TemplateEntity template;
    @Column(nullable = false, length = 160)
    private String title;
    @Column(nullable = false, length = 180)
    private String slug;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Status status = Status.DRAFT;
    @Column(name = "target_role", length = 120)
    private String targetRole;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "sections_json", nullable = false, columnDefinition = "jsonb")
    private String sectionsJson;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "theme_json", nullable = false, columnDefinition = "jsonb")
    private String themeJson;
    @Column(name = "version_number", nullable = false)
    private int versionNumber = 1;
    @Column(name = "last_exported_at")
    private Instant lastExportedAt;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ResumeEntity() {}

    public ResumeEntity(UserEntity owner, TemplateEntity template, String title, String slug, String sectionsJson, String themeJson) {
        this.owner = owner;
        this.template = template;
        this.title = title;
        this.slug = slug;
        this.sectionsJson = sectionsJson;
        this.themeJson = themeJson;
    }

    @PrePersist void create() { createdAt = Instant.now(); updatedAt = createdAt; }
    @PreUpdate void updateTime() { updatedAt = Instant.now(); }
    public UUID getId() { return id; }
    public UserEntity getOwner() { return owner; }
    public TemplateEntity getTemplate() { return template; }
    public String getTitle() { return title; }
    public String getSlug() { return slug; }
    public Status getStatus() { return status; }
    public String getTargetRole() { return targetRole; }
    public String getSectionsJson() { return sectionsJson; }
    public String getThemeJson() { return themeJson; }
    public int getVersionNumber() { return versionNumber; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void edit(String title, String targetRole, TemplateEntity template, String sectionsJson, String themeJson) {
        this.title = title;
        this.targetRole = targetRole;
        this.template = template;
        this.sectionsJson = sectionsJson;
        this.themeJson = themeJson;
    }
    public void publish() { status = Status.PUBLISHED; }
    public void incrementVersion() { versionNumber++; }
    public void exported() { lastExportedAt = Instant.now(); }
}
