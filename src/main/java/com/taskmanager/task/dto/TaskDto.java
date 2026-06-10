package com.taskmanager.task.dto;

import com.taskmanager.label.dto.LabelDto;
import com.taskmanager.task.entity.Priority;
import com.taskmanager.task.entity.RecurrenceFreq;

import java.time.Instant;
import java.util.List;

public record TaskDto(
        Long id,
        Long boardId,
        Long columnId,
        Long categoryId,
        Long assigneeId,
        String title,
        String description,
        Priority priority,
        Instant dueDate,
        Instant remindAt,
        RecurrenceFreq recurrenceFreq,
        Integer recurrenceInterval,
        Instant recurrenceUntil,
        int position,
        boolean completed,
        List<LabelDto> labels,
        int subtaskTotal,
        int subtaskDone,
        int attachmentCount,
        long trackedSeconds
) {
}
