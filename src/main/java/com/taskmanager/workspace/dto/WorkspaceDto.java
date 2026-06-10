package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;

public record WorkspaceDto(
        Long id,
        String name,
        Long ownerId,
        WorkspaceRole role,    // current user's role in this workspace
        int memberCount
) {
}
