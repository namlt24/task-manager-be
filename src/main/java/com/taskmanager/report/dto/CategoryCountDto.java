package com.taskmanager.report.dto;

public record CategoryCountDto(
        Long categoryId,
        String name,
        String color,
        long count
) {
}
