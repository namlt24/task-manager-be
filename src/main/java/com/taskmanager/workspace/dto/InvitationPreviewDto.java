package com.taskmanager.workspace.dto;

import com.taskmanager.workspace.entity.WorkspaceRole;

/**
 * What the accept page shows before the user joins. {@code valid} is false when the token is
 * unknown/expired/revoked or the invited email does not match the logged-in user; {@code reason}
 * carries a human message in that case.
 */
public record InvitationPreviewDto(
        Long workspaceId,
        String workspaceName,
        WorkspaceRole role,
        String invitedEmail,
        String inviterName,
        boolean valid,
        boolean alreadyMember,
        String reason
) {
}
