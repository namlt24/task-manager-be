package com.taskmanager.note.dto;

import java.time.Instant;

public record NoteDto(
        Long id,
        Long taskId,
        String title,
        String content,
        Instant createdAt,
        Instant updatedAt
) {
}
