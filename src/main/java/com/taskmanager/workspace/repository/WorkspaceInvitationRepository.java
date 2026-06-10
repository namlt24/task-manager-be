package com.taskmanager.workspace.repository;

import com.taskmanager.workspace.entity.InvitationStatus;
import com.taskmanager.workspace.entity.WorkspaceInvitation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByToken(String token);

    Optional<WorkspaceInvitation> findByWorkspaceIdAndEmailIgnoreCaseAndStatus(
            Long workspaceId, String email, InvitationStatus status);

    Optional<WorkspaceInvitation> findByIdAndWorkspaceId(Long id, Long workspaceId);

    List<WorkspaceInvitation> findByWorkspaceIdAndStatusOrderByIdDesc(Long workspaceId, InvitationStatus status);
}
