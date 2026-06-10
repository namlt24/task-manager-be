package com.taskmanager.subtask.controller;

import com.taskmanager.subtask.dto.SubtaskDto;
import com.taskmanager.subtask.dto.SubtaskRequest;
import com.taskmanager.subtask.dto.UpdateSubtaskRequest;
import com.taskmanager.subtask.service.SubtaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Subtasks")
public class SubtaskController {

    private final SubtaskService service;

    public SubtaskController(SubtaskService service) {
        this.service = service;
    }

    @GetMapping("/v1/tasks/{taskId}/subtasks")
    @Operation(summary = "List subtasks of a task")
    public List<SubtaskDto> list(@PathVariable Long taskId) {
        return service.list(taskId);
    }

    @PostMapping("/v1/tasks/{taskId}/subtasks")
    @Operation(summary = "Add a subtask")
    public ResponseEntity<SubtaskDto> create(@PathVariable Long taskId, @Valid @RequestBody SubtaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(taskId, request));
    }

    @PutMapping("/v1/subtasks/{id}")
    @Operation(summary = "Update a subtask (title and/or completed)")
    public SubtaskDto update(@PathVariable Long id, @Valid @RequestBody UpdateSubtaskRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/v1/subtasks/{id}")
    @Operation(summary = "Delete a subtask")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
