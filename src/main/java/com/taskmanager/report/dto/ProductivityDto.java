package com.taskmanager.report.dto;

import java.util.List;

public record ProductivityDto(
        List<DayStatDto> days
) {
}
