package com.taskmanager.board.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ColumnRequest(
        @NotBlank @Size(max = 120) String name
) {
}
