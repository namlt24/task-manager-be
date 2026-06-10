package com.taskmanager.report.dto;

import java.time.Instant;

public record CalendarItemDto(
        Long taskId,
        String title,
        Instant dueDate,
        boolean completed,
        String priority,
        String categoryColor
) {
}
