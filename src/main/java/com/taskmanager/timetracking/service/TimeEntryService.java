package com.taskmanager.timetracking.service;

import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.timetracking.dto.StartTimeEntryRequest;
import com.taskmanager.timetracking.dto.TimeEntryDto;
import com.taskmanager.timetracking.entity.TimeEntry;
import com.taskmanager.timetracking.entity.TimeEntrySource;
import com.taskmanager.timetracking.mapper.TimeEntryMapper;
import com.taskmanager.timetracking.repository.TimeEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
public class TimeEntryService {

    private final TimeEntryRepository repository;
    private final TaskRepository taskRepository;
    private final com.taskmanager.workspace.WorkspaceAccess access;
    private final TimeEntryMapper mapper;

    public TimeEntryService(TimeEntryRepository repository, TaskRepository taskRepository,
                            com.taskmanager.workspace.WorkspaceAccess access, TimeEntryMapper mapper) {
        this.repository = repository;
        this.taskRepository = taskRepository;
        this.access = access;
        this.mapper = mapper;
    }

    /** Starts a new session. Any already-running session for the user is stopped first (max 1 active). */
    @Transactional
    public TimeEntryDto start(StartTimeEntryRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        if (req.taskId() != null) {
            taskRepository.findByIdAndWorkspaceId(req.taskId(), access.currentWorkspaceId())
                    .orElseThrow(() -> new BadRequestException("Công việc không hợp lệ"));
        }
        // Auto-close any running session so there is only one active timer.
        repository.findFirstByUserIdAndEndedAtIsNull(userId).ifPresent(this::close);

        TimeEntry entry = new TimeEntry();
        entry.setUserId(userId);
        entry.setTaskId(req.taskId());
        entry.setSource(req.source() != null ? req.source() : TimeEntrySource.STOPWATCH);
        entry.setStartedAt(Instant.now());
        entry.setNote(req.note());
        return mapper.toDto(repository.save(entry));
    }

    @Transactional
    public TimeEntryDto stop(Long id) {
        TimeEntry entry = loadOwned(id);
        if (entry.getEndedAt() == null) {
            close(entry);
        }
        return mapper.toDto(entry);
    }

    @Transactional(readOnly = true)
    public TimeEntryDto getActive() {
        return repository.findFirstByUserIdAndEndedAtIsNull(SecurityUtils.getCurrentUserId())
                .map(mapper::toDto).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<TimeEntryDto> list(Long taskId, Instant from) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<TimeEntry> items;
        if (taskId != null) {
            items = repository.findByUserIdAndTaskIdOrderByStartedAtDesc(userId, taskId);
        } else if (from != null) {
            items = repository.findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtDesc(userId, from);
        } else {
            items = repository.findByUserIdOrderByStartedAtDesc(userId);
        }
        return items.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long totalSecondsForTask(Long taskId) {
        return repository.sumDurationByTaskId(taskId);
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(loadOwned(id));
    }

    private void close(TimeEntry entry) {
        Instant now = Instant.now();
        entry.setEndedAt(now);
        entry.setDurationSeconds((int) Math.max(0, Duration.between(entry.getStartedAt(), now).getSeconds()));
        repository.save(entry);
    }

    private TimeEntry loadOwned(Long id) {
        return repository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("TimeEntry", id));
    }
}
