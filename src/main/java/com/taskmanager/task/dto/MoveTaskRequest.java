package com.taskmanager.task.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record MoveTaskRequest(
        @NotNull Long columnId,
        @NotNull @PositiveOrZero Integer position
) {
}
