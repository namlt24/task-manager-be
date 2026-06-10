package com.taskmanager.task.controller;

import com.taskmanager.security.SecurityUtils;
import com.taskmanager.task.dto.AssignTaskRequest;
import com.taskmanager.task.dto.CreateTaskRequest;
import com.taskmanager.task.dto.MoveTaskRequest;
import com.taskmanager.task.dto.TaskDto;
import com.taskmanager.task.dto.UpdateTaskRequest;
import com.taskmanager.task.entity.Priority;
import com.taskmanager.task.service.TaskService;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/tasks")
@Tag(name = "Tasks")
public class TaskController {

    private final TaskService service;

    public TaskController(TaskService service) {
        this.service = service;
    }

    @GetMapping("/search")
    @Operation(summary = "Search/filter tasks (all params optional)")
    public List<TaskDto> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long boardId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long labelId,
            @RequestParam(required = false) Priority priority,
            @RequestParam(required = false) Boolean completed,
            @RequestParam(required = false) String dueFrom,
            @RequestParam(required = false) String dueTo,
            @RequestParam(required = false) String assignee) {
        Instant from = (dueFrom != null && !dueFrom.isBlank()) ? Instant.parse(dueFrom) : null;
        Instant to = (dueTo != null && !dueTo.isBlank()) ? Instant.parse(dueTo) : null;
        Long assigneeId = resolveAssignee(assignee);
        return service.search(q, boardId, categoryId, labelId, priority, completed, from, to, assigneeId);
    }

    /** Resolves the {@code assignee} filter: "me" → current user id, a number → that user id, else null. */
    private Long resolveAssignee(String assignee) {
        if (assignee == null || assignee.isBlank()) {
            return null;
        }
        if ("me".equalsIgnoreCase(assignee)) {
            return SecurityUtils.getCurrentUserId();
        }
        try {
            return Long.parseLong(assignee.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a task")
    public TaskDto get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping
    @Operation(summary = "Create a task")
    public ResponseEntity<TaskDto> create(@Valid @RequestBody CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update a task")
    public TaskDto update(@PathVariable Long id, @Valid @RequestBody UpdateTaskRequest request) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/move")
    @Operation(summary = "Move a task to a column/position")
    public TaskDto move(@PathVariable Long id, @Valid @RequestBody MoveTaskRequest request) {
        return service.move(id, request);
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Assign/unassign a task to a workspace member (MANAGER/OWNER)")
    public TaskDto assign(@PathVariable Long id, @RequestBody AssignTaskRequest request) {
        return service.assign(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a task")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
