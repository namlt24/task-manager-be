package com.taskmanager.attachment.dto;

import java.time.Instant;

public record AttachmentDto(
        Long id,
        Long taskId,
        String filename,
        String contentType,
        long size,
        Instant createdAt
) {
}
