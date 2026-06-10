package com.taskmanager.workspace;

import com.taskmanager.common.exception.ForbiddenException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.workspace.entity.Workspace;
import com.taskmanager.workspace.entity.WorkspaceMember;
import com.taskmanager.workspace.entity.WorkspaceRole;
import com.taskmanager.workspace.repository.WorkspaceMemberRepository;
import com.taskmanager.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Component;

/**
 * Resolves and authorizes the "current workspace" for a request. The id comes from the
 * {@code X-Workspace-Id} header ({@link WorkspaceContext}); if absent it falls back to the user's
 * personal (owned) workspace. Every access verifies the current user is a member.
 */
@Component
public class WorkspaceAccess {

    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;

    public WorkspaceAccess(WorkspaceMemberRepository memberRepository, WorkspaceRepository workspaceRepository) {
        this.memberRepository = memberRepository;
        this.workspaceRepository = workspaceRepository;
    }

    /** Current workspace id (verified the user is a member); falls back to the user's personal workspace. */
    public Long currentWorkspaceId() {
        Long userId = SecurityUtils.getCurrentUserId();
        Long requested = WorkspaceContext.get();
        if (requested != null) {
            if (!memberRepository.existsByWorkspaceIdAndUserId(requested, userId)) {
                throw new ForbiddenException("Bạn không thuộc workspace này");
            }
            return requested;
        }
        // fallback: the user's own personal workspace
        return workspaceRepository.findFirstByOwnerIdOrderByIdAsc(userId)
                .map(Workspace::getId)
                .orElseThrow(() -> new ForbiddenException("Người dùng chưa có workspace"));
    }

    public WorkspaceRole currentRole() {
        return roleOf(currentWorkspaceId(), SecurityUtils.getCurrentUserId());
    }

    /** Require the current user to be at least MANAGER (i.e. MANAGER or OWNER) in the current workspace. */
    public Long requireManager() {
        Long wsId = currentWorkspaceId();
        WorkspaceRole role = roleOf(wsId, SecurityUtils.getCurrentUserId());
        if (role == WorkspaceRole.MEMBER) {
            throw new ForbiddenException("Cần quyền quản lý (MANAGER/OWNER)");
        }
        return wsId;
    }

    /** Require the current user to be the OWNER of the current workspace. */
    public Long requireOwner() {
        Long wsId = currentWorkspaceId();
        if (roleOf(wsId, SecurityUtils.getCurrentUserId()) != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Chỉ chủ workspace (OWNER) được thực hiện");
        }
        return wsId;
    }

    /** Whether {@code userId} is a member of {@code workspaceId} (used to validate assignees). */
    public boolean isMember(Long workspaceId, Long userId) {
        return memberRepository.existsByWorkspaceIdAndUserId(workspaceId, userId);
    }

    public WorkspaceRole roleOf(Long workspaceId, Long userId) {
        return memberRepository.findByWorkspaceIdAndUserId(workspaceId, userId)
                .map(WorkspaceMember::getRole)
                .orElseThrow(() -> new ForbiddenException("Bạn không thuộc workspace này"));
    }

    // ---- explicit-id checks (for workspace-management endpoints that carry id in the path) ----

    public WorkspaceRole requireMemberOf(Long workspaceId) {
        return roleOf(workspaceId, SecurityUtils.getCurrentUserId());
    }

    public void requireManagerOf(Long workspaceId) {
        if (roleOf(workspaceId, SecurityUtils.getCurrentUserId()) == WorkspaceRole.MEMBER) {
            throw new ForbiddenException("Cần quyền quản lý (MANAGER/OWNER)");
        }
    }

    public void requireOwnerOf(Long workspaceId) {
        if (roleOf(workspaceId, SecurityUtils.getCurrentUserId()) != WorkspaceRole.OWNER) {
            throw new ForbiddenException("Chỉ chủ workspace (OWNER) được thực hiện");
        }
    }
}
