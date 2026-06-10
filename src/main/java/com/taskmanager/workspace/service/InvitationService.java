package com.taskmanager.workspace.service;

import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ConflictException;
import com.taskmanager.common.exception.ForbiddenException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.config.AppProperties;
import com.taskmanager.email.EmailService;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import com.taskmanager.workspace.dto.InvitationDto;
import com.taskmanager.workspace.dto.InvitationPreviewDto;
import com.taskmanager.workspace.dto.InviteRequest;
import com.taskmanager.workspace.dto.WorkspaceDto;
import com.taskmanager.workspace.entity.InvitationStatus;
import com.taskmanager.workspace.entity.Workspace;
import com.taskmanager.workspace.entity.WorkspaceInvitation;
import com.taskmanager.workspace.entity.WorkspaceMember;
import com.taskmanager.workspace.entity.WorkspaceRole;
import com.taskmanager.workspace.repository.WorkspaceInvitationRepository;
import com.taskmanager.workspace.repository.WorkspaceMemberRepository;
import com.taskmanager.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    /** How long an invitation link stays valid. */
    public static final Duration TTL = Duration.ofDays(7);

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;
    private final EmailService emailService;
    private final AppProperties properties;

    public InvitationService(WorkspaceInvitationRepository invitationRepository,
                             WorkspaceMemberRepository memberRepository,
                             WorkspaceRepository workspaceRepository,
                             UserRepository userRepository,
                             WorkspaceAccess access,
                             EmailService emailService,
                             AppProperties properties) {
        this.invitationRepository = invitationRepository;
        this.memberRepository = memberRepository;
        this.workspaceRepository = workspaceRepository;
        this.userRepository = userRepository;
        this.access = access;
        this.emailService = emailService;
        this.properties = properties;
    }

    private static final java.util.Map<WorkspaceRole, String> ROLE_LABELS = java.util.Map.of(
            WorkspaceRole.OWNER, "Chủ sở hữu",
            WorkspaceRole.MANAGER, "Quản lý",
            WorkspaceRole.MEMBER, "Thành viên");

    /** Manager/owner invites an email to the workspace. Reuses (resends) an existing pending invite. */
    @Transactional
    public InvitationDto invite(Long workspaceId, InviteRequest req) {
        access.requireManagerOf(workspaceId);
        Workspace ws = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", workspaceId));

        WorkspaceRole role = req.role();
        if (role == WorkspaceRole.OWNER) {
            throw new BadRequestException("Không thể mời với vai trò OWNER");
        }
        String email = req.email().toLowerCase().trim();

        // Already a member? (only if that email belongs to a registered, joined user)
        User existing = userRepository.findByEmailIgnoreCase(email).orElse(null);
        if (existing != null && memberRepository.existsByWorkspaceIdAndUserId(workspaceId, existing.getId())) {
            throw new ConflictException("Người dùng đã là thành viên của workspace");
        }

        WorkspaceInvitation inv = invitationRepository
                .findByWorkspaceIdAndEmailIgnoreCaseAndStatus(workspaceId, email, InvitationStatus.PENDING)
                .orElseGet(WorkspaceInvitation::new);
        inv.setWorkspaceId(workspaceId);
        inv.setEmail(email);
        inv.setRole(role);
        inv.setStatus(InvitationStatus.PENDING);
        inv.setInvitedBy(SecurityUtils.getCurrentUserId());
        inv.setToken(UUID.randomUUID().toString().replace("-", ""));
        inv.setExpiresAt(Instant.now().plus(TTL));
        inv = invitationRepository.save(inv);

        String inviterName = userRepository.findById(SecurityUtils.getCurrentUserId())
                .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail())
                .orElse(null);
        String acceptUrl = properties.getFrontendUrl() + "/invite/accept?token=" + inv.getToken();
        emailService.sendWorkspaceInvitation(email, inviterName, ws.getName(),
                ROLE_LABELS.getOrDefault(role, role.name()), acceptUrl);

        return toDto(inv);
    }

    @Transactional(readOnly = true)
    public List<InvitationDto> listPending(Long workspaceId) {
        access.requireManagerOf(workspaceId);
        return invitationRepository
                .findByWorkspaceIdAndStatusOrderByIdDesc(workspaceId, InvitationStatus.PENDING)
                .stream().map(this::toDto).toList();
    }

    @Transactional
    public void revoke(Long workspaceId, Long invitationId) {
        access.requireManagerOf(workspaceId);
        WorkspaceInvitation inv = invitationRepository.findByIdAndWorkspaceId(invitationId, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation", invitationId));
        if (inv.getStatus() == InvitationStatus.PENDING) {
            inv.setStatus(InvitationStatus.REVOKED);
            invitationRepository.save(inv);
        }
    }

    /** Read-only view for the accept page; never throws on a bad token (returns valid=false instead). */
    @Transactional(readOnly = true)
    public InvitationPreviewDto preview(String token) {
        WorkspaceInvitation inv = invitationRepository.findByToken(token).orElse(null);
        if (inv == null) {
            return invalid("Lời mời không tồn tại");
        }
        Workspace ws = workspaceRepository.findById(inv.getWorkspaceId()).orElse(null);
        String wsName = ws != null ? ws.getName() : null;
        String inviterName = inv.getInvitedBy() == null ? null
                : userRepository.findById(inv.getInvitedBy())
                        .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail())
                        .orElse(null);

        String reason = validationReason(inv);
        User me = userRepository.findById(SecurityUtils.getCurrentUserId()).orElse(null);
        boolean emailMatches = me != null && me.getEmail().equalsIgnoreCase(inv.getEmail());
        if (reason == null && !emailMatches) {
            reason = "Lời mời này dành cho " + inv.getEmail() + ". Hãy đăng nhập bằng email đó.";
        }
        boolean alreadyMember = me != null
                && memberRepository.existsByWorkspaceIdAndUserId(inv.getWorkspaceId(), me.getId());

        return new InvitationPreviewDto(inv.getWorkspaceId(), wsName, inv.getRole(), inv.getEmail(),
                inviterName, reason == null, alreadyMember, reason);
    }

    /** The logged-in user accepts the invite and becomes a member. Returns the joined workspace. */
    @Transactional
    public WorkspaceDto accept(String token) {
        WorkspaceInvitation inv = invitationRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Lời mời không tồn tại"));
        String reason = validationReason(inv);
        if (reason != null) {
            throw new BadRequestException(reason);
        }
        User me = userRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new BadRequestException("Tài khoản không tồn tại"));
        if (!me.getEmail().equalsIgnoreCase(inv.getEmail())) {
            throw new ForbiddenException("Lời mời này dành cho " + inv.getEmail()
                    + ". Hãy đăng nhập bằng email đó.");
        }

        Workspace ws = workspaceRepository.findById(inv.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Workspace", inv.getWorkspaceId()));

        WorkspaceMember member = memberRepository
                .findByWorkspaceIdAndUserId(inv.getWorkspaceId(), me.getId())
                .orElseGet(() -> {
                    WorkspaceMember m = new WorkspaceMember();
                    m.setWorkspaceId(inv.getWorkspaceId());
                    m.setUserId(me.getId());
                    m.setRole(inv.getRole());
                    return memberRepository.save(m);
                });

        inv.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(inv);

        int count = memberRepository.findByWorkspaceIdOrderByRoleAscIdAsc(ws.getId()).size();
        return new WorkspaceDto(ws.getId(), ws.getName(), ws.getOwnerId(), member.getRole(), count);
    }

    /** Returns a reason string if the invitation cannot be accepted, or null if it is acceptable. */
    private String validationReason(WorkspaceInvitation inv) {
        if (inv.getStatus() == InvitationStatus.ACCEPTED) {
            return "Lời mời đã được chấp nhận";
        }
        if (inv.getStatus() == InvitationStatus.REVOKED) {
            return "Lời mời đã bị thu hồi";
        }
        if (inv.getExpiresAt().isBefore(Instant.now())) {
            return "Lời mời đã hết hạn";
        }
        return null;
    }

    private InvitationPreviewDto invalid(String reason) {
        return new InvitationPreviewDto(null, null, null, null, null, false, false, reason);
    }

    private InvitationDto toDto(WorkspaceInvitation inv) {
        return new InvitationDto(inv.getId(), inv.getEmail(), inv.getRole(),
                inv.getStatus().name(), inv.getExpiresAt(), inv.getCreatedAt());
    }
}
