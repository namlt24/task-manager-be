package com.taskmanager.board.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderColumnsRequest(
        @NotEmpty List<Long> columnIds
) {
}
