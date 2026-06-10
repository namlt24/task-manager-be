package com.taskmanager.task.dto;

import com.taskmanager.task.entity.Priority;
import com.taskmanager.task.entity.RecurrenceFreq;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record UpdateTaskRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        Priority priority,
        Instant dueDate,
        Instant remindAt,
        RecurrenceFreq recurrenceFreq,
        Integer recurrenceInterval,
        Instant recurrenceUntil,
        Long categoryId,
        List<Long> labelIds,
        Boolean completed
) {
}
