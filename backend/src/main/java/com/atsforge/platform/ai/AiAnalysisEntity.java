package com.atsforge.platform.ai;

import com.atsforge.platform.resume.ResumeEntity;
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
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ai_analyses")
public class AiAnalysisEntity {
    public enum Status { QUEUED, PROCESSING, COMPLETED, FAILED }

    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "resume_id")
    private ResumeEntity resume;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "requested_by")
    private UserEntity requestedBy;
    @Column(name = "analysis_type", nullable = false, length = 30)
    private String analysisType;
    @Column(name = "job_description", columnDefinition = "TEXT")
    private String jobDescription;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Status status = Status.QUEUED;
    @Column(name = "ats_score")
    private Integer atsScore;
    @Column(name = "match_percentage")
    private Integer matchPercentage;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "result_json", columnDefinition = "jsonb")
    private String resultJson;
    @Column(length = 80)
    private String model;
    @Column(name = "input_tokens")
    private Integer inputTokens;
    @Column(name = "output_tokens")
    private Integer outputTokens;
    @Column(name = "error_message", length = 500)
    private String errorMessage;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "completed_at")
    private Instant completedAt;

    protected AiAnalysisEntity() {}
    public AiAnalysisEntity(ResumeEntity resume, UserEntity user, String jobDescription) {
        this.resume = resume;
        this.requestedBy = user;
        this.analysisType = jobDescription == null || jobDescription.isBlank() ? "ATS_REVIEW" : "JOB_MATCH";
        this.jobDescription = jobDescription;
    }
    @PrePersist void create() { createdAt = Instant.now(); }
    public UUID getId() { return id; }
    public ResumeEntity getResume() { return resume; }
    public String getJobDescription() { return jobDescription; }
    public Status getStatus() { return status; }
    public Integer getAtsScore() { return atsScore; }
    public Integer getMatchPercentage() { return matchPercentage; }
    public String getResultJson() { return resultJson; }
    public String getModel() { return model; }
    public Instant getCreatedAt() { return createdAt; }
    public void processing() { status = Status.PROCESSING; }
    public void complete(int score, int match, String resultJson, String model, Integer inputTokens, Integer outputTokens) {
        this.status = Status.COMPLETED;
        this.atsScore = score;
        this.matchPercentage = match;
        this.resultJson = resultJson;
        this.model = model;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.completedAt = Instant.now();
    }
    public void fail(String error) {
        this.status = Status.FAILED;
        this.errorMessage = error.substring(0, Math.min(500, error.length()));
        this.completedAt = Instant.now();
    }
}
