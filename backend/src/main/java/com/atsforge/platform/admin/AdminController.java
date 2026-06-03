package com.atsforge.platform.admin;

import com.atsforge.platform.ai.AiAnalysisRepository;
import com.atsforge.platform.billing.SubscriptionEntity;
import com.atsforge.platform.billing.SubscriptionRepository;
import com.atsforge.platform.common.AuditLogEntity;
import com.atsforge.platform.common.AuditLogRepository;
import com.atsforge.platform.export.ExportJobRepository;
import com.atsforge.platform.resume.ResumeRepository;
import com.atsforge.platform.user.UserEntity;
import com.atsforge.platform.user.UserRepository;
import com.atsforge.platform.user.UserRole;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private final UserRepository users;
    private final ResumeRepository resumes;
    private final AiAnalysisRepository analyses;
    private final ExportJobRepository exports;
    private final SubscriptionRepository subscriptions;
    private final AuditLogRepository logs;

    public AdminController(UserRepository users, ResumeRepository resumes, AiAnalysisRepository analyses,
                           ExportJobRepository exports, SubscriptionRepository subscriptions, AuditLogRepository logs) {
        this.users = users; this.resumes = resumes; this.analyses = analyses; this.exports = exports;
        this.subscriptions = subscriptions; this.logs = logs;
    }

    @GetMapping("/metrics")
    @Transactional(readOnly = true)
    public Metrics metrics() {
        return new Metrics(users.count(), resumes.count(), analyses.count(), exports.count(),
                subscriptions.countByPlanAndStatus(SubscriptionEntity.Plan.PRO, SubscriptionEntity.Status.ACTIVE));
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public Page<UserSummary> users(Pageable pageable) {
        return users.findAll(pageable).map(u -> new UserSummary(u.getId(), u.getEmail(), u.getDisplayName(), u.getRole(), u.getCreatedAt()));
    }

    @PatchMapping("/users/{id}/role")
    @Transactional
    public UserSummary role(@PathVariable UUID id, @RequestBody RoleUpdate request) {
        UserEntity user = users.findById(id).orElseThrow();
        user.setRole(request.role());
        return new UserSummary(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole(), user.getCreatedAt());
    }

    @GetMapping("/audit-logs")
    public Page<AuditResponse> audit(Pageable pageable) {
        return logs.findAllByOrderByCreatedAtDesc(pageable).map(AuditResponse::from);
    }

    public record Metrics(long users, long resumes, long aiAnalyses, long exports, long activeProSubscriptions) {}
    public record UserSummary(UUID id, String email, String displayName, UserRole role, Instant createdAt) {}
    public record RoleUpdate(@NotNull UserRole role) {}
    public record AuditResponse(String action, String entityType, UUID entityId, Instant createdAt) {
        static AuditResponse from(AuditLogEntity log) {
            return new AuditResponse(log.getAction(), log.getEntityType(), log.getEntityId(), log.getCreatedAt());
        }
    }
}
