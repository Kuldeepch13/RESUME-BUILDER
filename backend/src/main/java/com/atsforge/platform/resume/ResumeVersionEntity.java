package com.atsforge.platform.resume;

import com.atsforge.platform.user.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "resume_versions")
public class ResumeVersionEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id")
    private ResumeEntity resume;
    @Column(name = "version_number", nullable = false)
    private int versionNumber;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "sections_json", nullable = false, columnDefinition = "jsonb")
    private String sectionsJson;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "theme_json", nullable = false, columnDefinition = "jsonb")
    private String themeJson;
    @Column(name = "change_summary", length = 240)
    private String changeSummary;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "created_by")
    private UserEntity createdBy;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ResumeVersionEntity() {}
    public ResumeVersionEntity(ResumeEntity resume, UserEntity createdBy, String summary) {
        this.resume = resume;
        this.versionNumber = resume.getVersionNumber();
        this.sectionsJson = resume.getSectionsJson();
        this.themeJson = resume.getThemeJson();
        this.createdBy = createdBy;
        this.changeSummary = summary;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public int getVersionNumber() { return versionNumber; }
    public String getSectionsJson() { return sectionsJson; }
    public String getThemeJson() { return themeJson; }
    public String getChangeSummary() { return changeSummary; }
    public Instant getCreatedAt() { return createdAt; }
}
