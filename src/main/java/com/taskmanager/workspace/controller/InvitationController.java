package com.taskmanager.workspace.controller;

import com.taskmanager.workspace.dto.AcceptInvitationRequest;
import com.taskmanager.workspace.dto.InvitationPreviewDto;
import com.taskmanager.workspace.dto.WorkspaceDto;
import com.taskmanager.workspace.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Token-based invitation endpoints (not scoped to a workspace path). The logged-in user previews
 * an invitation link and accepts it to join the workspace.
 */
@RestController
@RequestMapping("/v1/invitations")
@Tag(name = "Invitations")
public class InvitationController {

    private final InvitationService service;

    public InvitationController(InvitationService service) {
        this.service = service;
    }

    @GetMapping("/{token}")
    @Operation(summary = "Preview an invitation by token (for the accept page)")
    public InvitationPreviewDto preview(@PathVariable String token) {
        return service.preview(token);
    }

    @PostMapping("/accept")
    @Operation(summary = "Accept an invitation and join the workspace")
    public WorkspaceDto accept(@Valid @RequestBody AcceptInvitationRequest request) {
        return service.accept(request.token());
    }
}
