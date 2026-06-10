package com.taskmanager.note.service;

import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.note.dto.NoteDto;
import com.taskmanager.note.dto.NoteRequest;
import com.taskmanager.note.entity.Note;
import com.taskmanager.note.mapper.NoteMapper;
import com.taskmanager.note.repository.NoteRepository;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NoteService {

    private final NoteRepository repository;
    private final TaskRepository taskRepository;
    private final WorkspaceAccess access;
    private final NoteMapper mapper;

    public NoteService(NoteRepository repository, TaskRepository taskRepository,
                       WorkspaceAccess access, NoteMapper mapper) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.access = access;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<NoteDto> list(Long taskId) {
        Long workspaceId = access.currentWorkspaceId();
        List<Note> notes = taskId != null
                ? repository.findByWorkspaceIdAndTaskIdOrderByUpdatedAtDesc(workspaceId, taskId)
                : repository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId);
        return notes.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public NoteDto get(Long id) {
        return mapper.toDto(loadOwned(id));
    }

    @Transactional
    public NoteDto create(NoteRequest request) {
        Long workspaceId = access.currentWorkspaceId();
        Note note = new Note();
        note.setUserId(SecurityUtils.getCurrentUserId());
        note.setWorkspaceId(workspaceId);
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTaskId(resolveTask(request.taskId(), workspaceId));
        return mapper.toDto(repository.save(note));
    }

    @Transactional
    public NoteDto update(Long id, NoteRequest request) {
        Note note = loadOwned(id);
        note.setTitle(request.title());
        note.setContent(request.content());
        note.setTaskId(resolveTask(request.taskId(), note.getWorkspaceId()));
        return mapper.toDto(repository.save(note));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(loadOwned(id));
    }

    private Note loadOwned(Long id) {
        return repository.findByIdAndWorkspaceId(id, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Note", id));
    }

    private Long resolveTask(Long taskId, Long workspaceId) {
        if (taskId == null) {
            return null;
        }
        taskRepository.findByIdAndWorkspaceId(taskId, workspaceId)
                .orElseThrow(() -> new BadRequestException("Công việc liên kết không hợp lệ"));
        return taskId;
    }
}
