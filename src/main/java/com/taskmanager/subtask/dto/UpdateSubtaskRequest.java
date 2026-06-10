package com.taskmanager.subtask.dto;

import jakarta.validation.constraints.Size;

public record UpdateSubtaskRequest(
        @Size(max = 255) String title,
        Boolean completed
) {
}
