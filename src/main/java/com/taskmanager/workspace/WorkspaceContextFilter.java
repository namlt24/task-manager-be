package com.taskmanager.workspace;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/** Reads the {@code X-Workspace-Id} header into {@link WorkspaceContext} for the duration of the request. */
@Component
public class WorkspaceContextFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Workspace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String value = request.getHeader(HEADER);
        if (value != null && !value.isBlank()) {
            try {
                WorkspaceContext.set(Long.parseLong(value.trim()));
            } catch (NumberFormatException ignored) {
                // invalid header -> treat as absent (falls back to default workspace downstream)
            }
        }
        try {
            chain.doFilter(request, response);
        } finally {
            WorkspaceContext.clear();
        }
    }
}
