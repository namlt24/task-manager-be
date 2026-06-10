package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.NotNull;

public record UpdateMemberRoleRequest(
        @NotNull WorkspaceRole role
) {
}
