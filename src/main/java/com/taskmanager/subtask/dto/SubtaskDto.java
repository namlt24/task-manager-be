package com.taskmanager.subtask.dto;

public record SubtaskDto(
        Long id,
        Long taskId,
        String title,
        boolean completed,
        int position
) {
}
