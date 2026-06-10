package com.taskmanager.report.dto;

public record DayStatDto(
        String date,          // yyyy-MM-dd (user timezone)
        long created,
        long completed,
        long trackedSeconds
) {
}
