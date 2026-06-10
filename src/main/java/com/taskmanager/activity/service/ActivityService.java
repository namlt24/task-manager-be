package com.taskmanager.activity.service;

import com.taskmanager.activity.dto.ActivityDto;
import com.taskmanager.activity.entity.ActivityLog;
import com.taskmanager.activity.repository.ActivityLogRepository;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ActivityService {

    private final ActivityLogRepository repository;
    private final UserRepository userRepository;
    private final WorkspaceAccess access;

    public ActivityService(ActivityLogRepository repository, UserRepository userRepository,
                           WorkspaceAccess access) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.access = access;
    }

    /** Persists an activity row (called by the Kafka consumer; no security context). */
    @Transactional
    public void record(Long workspaceId, Long actorId, String type, Long taskId,
                       Long targetUserId, String message) {
        ActivityLog log = new ActivityLog();
        log.setWorkspaceId(workspaceId);
        log.setActorId(actorId);
        log.setType(type);
        log.setTaskId(taskId);
        log.setTargetUserId(targetUserId);
        log.setMessage(message);
        repository.save(log);
    }

    @Transactional(readOnly = true)
    public List<ActivityDto> list(Long workspaceId) {
        access.requireMemberOf(workspaceId);
        List<ActivityLog> items = repository.findTop50ByWorkspaceIdOrderByIdDesc(workspaceId);
        Map<Long, User> actors = userRepository.findAllById(
                        items.stream().map(ActivityLog::getActorId).filter(java.util.Objects::nonNull).toList())
                .stream().collect(Collectors.toMap(User::getId, Function.identity()));
        return items.stream().map(a -> {
            User actor = a.getActorId() == null ? null : actors.get(a.getActorId());
            String name = actor == null ? null
                    : (actor.getFullName() != null && !actor.getFullName().isBlank()
                        ? actor.getFullName() : actor.getEmail());
            return new ActivityDto(a.getId(), a.getType(), a.getTaskId(), a.getActorId(), name,
                    a.getMessage(), a.getCreatedAt());
        }).toList();
    }
}
