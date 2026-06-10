package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InviteRequest(
        @NotBlank @Email @Size(max = 150) String email,
        @NotNull WorkspaceRole role
) {
}
