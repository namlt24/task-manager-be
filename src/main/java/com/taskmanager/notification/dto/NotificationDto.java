package com.taskmanager.notification.dto;

import com.taskmanager.notification.entity.NotificationType;

import java.time.Instant;

public record NotificationDto(
        Long id,
        Long taskId,
        NotificationType type,
        String title,
        String message,
        boolean read,
        Instant createdAt
) {
}
