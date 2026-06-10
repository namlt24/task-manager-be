package com.taskmanager.task.dto;

import com.taskmanager.task.entity.Priority;
import com.taskmanager.task.entity.RecurrenceFreq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record CreateTaskRequest(
        @NotNull Long boardId,
        @NotNull Long columnId,
        @NotBlank @Size(max = 255) String title,
        String description,
        Priority priority,
        Instant dueDate,
        Instant remindAt,
        RecurrenceFreq recurrenceFreq,
        Integer recurrenceInterval,
        Instant recurrenceUntil,
        Long categoryId,
        Long assigneeId,
        List<Long> labelIds
) {
}
