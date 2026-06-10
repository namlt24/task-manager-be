package com.taskmanager.category.dto;

public record CategoryDto(
        Long id,
        String name,
        String color,
        String icon,
        int position
) {
}
