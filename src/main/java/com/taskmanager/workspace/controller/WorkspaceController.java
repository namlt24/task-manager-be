package com.taskmanager.workspace.controller;

import com.taskmanager.workspace.dto.CreateWorkspaceRequest;
import com.taskmanager.workspace.dto.InvitationDto;
import com.taskmanager.workspace.dto.InviteRequest;
import com.taskmanager.workspace.dto.UpdateMemberRoleRequest;
import com.taskmanager.workspace.dto.WorkspaceDto;
import com.taskmanager.workspace.dto.WorkspaceMemberDto;
import com.taskmanager.workspace.service.InvitationService;
import com.taskmanager.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/workspaces")
@Tag(name = "Workspaces")
public class WorkspaceController {

    private final WorkspaceService service;
    private final InvitationService invitationService;

    public WorkspaceController(WorkspaceService service, InvitationService invitationService) {
        this.service = service;
        this.invitationService = invitationService;
    }

    @GetMapping
    @Operation(summary = "List workspaces the current user belongs to")
    public List<WorkspaceDto> list() {
        return service.listMine();
    }

    @PostMapping
    @Operation(summary = "Create a workspace (creator becomes OWNER)")
    public ResponseEntity<WorkspaceDto> create(@Valid @RequestBody CreateWorkspaceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping("/{id}/members")
    @Operation(summary = "List members of a workspace")
    public List<WorkspaceMemberDto> members(@PathVariable Long id) {
        return service.members(id);
    }

    @PatchMapping("/{id}/members/{userId}/role")
    @Operation(summary = "Change a member's role (OWNER only)")
    public ResponseEntity<Void> changeRole(@PathVariable Long id, @PathVariable Long userId,
                                           @Valid @RequestBody UpdateMemberRoleRequest request) {
        service.changeRole(id, userId, request.role());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/members/{userId}")
    @Operation(summary = "Remove a member (OWNER only)")
    public ResponseEntity<Void> removeMember(@PathVariable Long id, @PathVariable Long userId) {
        service.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    // ---- Invitations (MANAGER/OWNER) ----

    @GetMapping("/{id}/invitations")
    @Operation(summary = "List pending invitations of a workspace (MANAGER/OWNER)")
    public List<InvitationDto> invitations(@PathVariable Long id) {
        return invitationService.listPending(id);
    }

    @PostMapping("/{id}/invitations")
    @Operation(summary = "Invite someone to the workspace by email (MANAGER/OWNER)")
    public ResponseEntity<InvitationDto> invite(@PathVariable Long id,
                                                @Valid @RequestBody InviteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.invite(id, request));
    }

    @DeleteMapping("/{id}/invitations/{invitationId}")
    @Operation(summary = "Revoke a pending invitation (MANAGER/OWNER)")
    public ResponseEntity<Void> revokeInvitation(@PathVariable Long id, @PathVariable Long invitationId) {
        invitationService.revoke(id, invitationId);
        return ResponseEntity.noContent().build();
    }
}
