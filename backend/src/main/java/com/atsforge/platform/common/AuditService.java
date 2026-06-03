package com.atsforge.platform.common;

import com.atsforge.platform.security.SecurityUtils;
import com.atsforge.platform.user.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final AuditLogRepository logs;
    private final UserRepository users;
    public AuditService(AuditLogRepository logs, UserRepository users) { this.logs = logs; this.users = users; }
    public void record(String action, String entityType, UUID entityId) {
        logs.save(new AuditLogEntity(users.getReferenceById(SecurityUtils.currentUserId()), action, entityType, entityId, "{}"));
    }
}

