package com.taskmanager.board.dto;

public record BoardDto(
        Long id,
        String name,
        int position
) {
}
