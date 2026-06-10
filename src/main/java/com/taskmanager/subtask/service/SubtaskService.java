package com.taskmanager.subtask.service;

import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.subtask.dto.SubtaskDto;
import com.taskmanager.subtask.dto.SubtaskRequest;
import com.taskmanager.subtask.dto.UpdateSubtaskRequest;
import com.taskmanager.subtask.entity.Subtask;
import com.taskmanager.subtask.mapper.SubtaskMapper;
import com.taskmanager.subtask.repository.SubtaskRepository;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SubtaskService {

    private final SubtaskRepository repository;
    private final TaskRepository taskRepository;
    private final WorkspaceAccess access;
    private final SubtaskMapper mapper;

    public SubtaskService(SubtaskRepository repository, TaskRepository taskRepository,
                          WorkspaceAccess access, SubtaskMapper mapper) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.access = access;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<SubtaskDto> list(Long taskId) {
        requireTaskInWorkspace(taskId);
        return repository.findByTaskIdOrderByPositionAscIdAsc(taskId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @Transactional
    public SubtaskDto create(Long taskId, SubtaskRequest request) {
        requireTaskInWorkspace(taskId);
        Subtask subtask = new Subtask();
        subtask.setTaskId(taskId);
        subtask.setUserId(SecurityUtils.getCurrentUserId());
        subtask.setTitle(request.title());
        subtask.setPosition((int) repository.countByTaskId(taskId));
        return mapper.toDto(repository.save(subtask));
    }

    @Transactional
    public SubtaskDto update(Long id, UpdateSubtaskRequest request) {
        Subtask subtask = loadOwned(id);
        if (request.title() != null && !request.title().isBlank()) {
            subtask.setTitle(request.title());
        }
        if (request.completed() != null) {
            subtask.setCompleted(request.completed());
        }
        return mapper.toDto(repository.save(subtask));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(loadOwned(id));
    }

    private Subtask loadOwned(Long id) {
        Subtask subtask = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subtask", id));
        requireTaskInWorkspace(subtask.getTaskId());
        return subtask;
    }

    /** Ensures the task exists in the current workspace (caller is a member). */
    private void requireTaskInWorkspace(Long taskId) {
        taskRepository.findByIdAndWorkspaceId(taskId, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }
}
