package com.atsforge.platform.security;

import com.atsforge.platform.common.ForbiddenException;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {}

    public static UUID currentUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof UserPrincipal user) {
            return user.id();
        }
        throw new ForbiddenException("Authentication required.");
    }
}

