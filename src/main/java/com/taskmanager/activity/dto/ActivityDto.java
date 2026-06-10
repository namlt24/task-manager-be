package com.taskmanager.activity.dto;

import java.time.Instant;

public record ActivityDto(
        Long id,
        String type,
        Long taskId,
        Long actorId,
        String actorName,
        String message,
        Instant createdAt
) {
}
