package com.taskmanager.board.dto;

import java.util.List;

public record BoardDetailDto(
        Long id,
        String name,
        List<ColumnDto> columns
) {
}
