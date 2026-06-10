package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;

import java.time.Instant;

public record InvitationDto(
        Long id,
        String email,
        WorkspaceRole role,
        String status,
        Instant expiresAt,
        Instant createdAt
) {
}
