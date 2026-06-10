package com.taskmanager.workspace;

/**
 * Holds the workspace id requested for the current request (from the {@code X-Workspace-Id} header),
 * set by {@link WorkspaceContextFilter} and cleared at the end of the request.
 */
public final class WorkspaceContext {

    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();

    private WorkspaceContext() {
    }

    public static void set(Long workspaceId) {
        CURRENT.set(workspaceId);
    }

    /** Requested workspace id, or {@code null} if the header was absent/invalid. */
    public static Long get() {
        return CURRENT.get();
    }

    public static void clear() {
        CURRENT.remove();
    }
}
