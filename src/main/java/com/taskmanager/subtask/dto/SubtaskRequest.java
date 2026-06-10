package com.taskmanager.subtask.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SubtaskRequest(
        @NotBlank @Size(max = 255) String title
) {
}
