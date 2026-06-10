package com.taskmanager.workspace.service;

import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import com.taskmanager.workspace.dto.CreateWorkspaceRequest;
import com.taskmanager.workspace.dto.WorkspaceDto;
import com.taskmanager.workspace.dto.WorkspaceMemberDto;
import com.taskmanager.workspace.entity.Workspace;
import com.taskmanager.workspace.entity.WorkspaceMember;
import com.taskmanager.workspace.entity.WorkspaceRole;
import com.taskmanager.workspace.repository.WorkspaceMemberRepository;
import com.taskmanager.workspace.repository.WorkspaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository memberRepository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;

    public WorkspaceService(WorkspaceRepository workspaceRepository,
                            WorkspaceMemberRepository memberRepository,
                            UserRepository userRepository,
                            WorkspaceAccess access) {
        this.workspaceRepository = workspaceRepository;
        this.memberRepository = memberRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    /** Provisions a workspace owned by {@code userId} (used at registration). Returns the new workspace. */
    @Transactional
    public Workspace provision(Long userId, String name) {
        Workspace ws = new Workspace();
        ws.setName(name);
        ws.setOwnerId(userId);
        ws = workspaceRepository.save(ws);
        WorkspaceMember owner = new WorkspaceMember();
        owner.setWorkspaceId(ws.getId());
        owner.setUserId(userId);
        owner.setRole(WorkspaceRole.OWNER);
        memberRepository.save(owner);
        return ws;
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto> listMine() {
        Long userId = SecurityUtils.getCurrentUserId();
        return memberRepository.findByUserId(userId).stream()
                .map(m -> {
                    Workspace ws = workspaceRepository.findById(m.getWorkspaceId()).orElse(null);
                    if (ws == null) {
                        return null;
                    }
                    int count = memberRepository.findByWorkspaceIdOrderByRoleAscIdAsc(ws.getId()).size();
                    return new WorkspaceDto(ws.getId(), ws.getName(), ws.getOwnerId(), m.getRole(), count);
                })
                .filter(java.util.Objects::nonNull)
                .sorted(java.util.Comparator.comparing(WorkspaceDto::id))
                .toList();
    }

    @Transactional
    public WorkspaceDto create(CreateWorkspaceRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        Workspace ws = provision(userId, req.name());
        return new WorkspaceDto(ws.getId(), ws.getName(), ws.getOwnerId(), WorkspaceRole.OWNER, 1);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceMemberDto> members(Long workspaceId) {
        access.requireMemberOf(workspaceId);
        List<WorkspaceMember> members = memberRepository.findByWorkspaceIdOrderByRoleAscIdAsc(workspaceId);
        Map<Long, User> users = userRepository.findAllById(
                        members.stream().map(WorkspaceMember::getUserId).toList()).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));
        return members.stream().map(m -> {
            User u = users.get(m.getUserId());
            return new WorkspaceMemberDto(m.getUserId(),
                    u != null ? u.getEmail() : null,
                    u != null ? u.getFullName() : null,
                    m.getRole());
        }).toList();
    }

    @Transactional
    public void changeRole(Long workspaceId, Long targetUserId, WorkspaceRole role) {
        access.requireOwnerOf(workspaceId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", targetUserId));
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Không thể đổi vai trò của chủ workspace");
        }
        if (role == WorkspaceRole.OWNER) {
            throw new BadRequestException("Không thể gán vai trò OWNER (chỉ có 1 chủ)");
        }
        member.setRole(role);
        memberRepository.save(member);
    }

    @Transactional
    public void removeMember(Long workspaceId, Long targetUserId) {
        access.requireOwnerOf(workspaceId);
        WorkspaceMember member = memberRepository.findByWorkspaceIdAndUserId(workspaceId, targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Member", targetUserId));
        if (member.getRole() == WorkspaceRole.OWNER) {
            throw new BadRequestException("Không thể xoá chủ workspace");
        }
        memberRepository.delete(member);
    }
}
