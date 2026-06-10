package com.taskmanager.security;

import com.taskmanager.common.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Convenience accessors for the currently authenticated principal.
 */
public final class SecurityUtils {

    private SecurityUtils() {
    }

    public static AppUserDetails getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserDetails details)) {
            throw new BadRequestException("No authenticated user in context");
        }
        return details;
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
