package com.taskmanager.board.dto;

import com.taskmanager.task.dto.TaskDto;

import java.util.List;

public record ColumnDto(
        Long id,
        String name,
        int position,
        List<TaskDto> tasks
) {
}
