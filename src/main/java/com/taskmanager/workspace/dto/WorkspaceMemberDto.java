package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;

public record WorkspaceMemberDto(
        Long userId,
        String email,
        String fullName,
        WorkspaceRole role
) {
}
