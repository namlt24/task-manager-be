package com.taskmanager.timetracking.dto;

import com.taskmanager.timetracking.entity.TimeEntrySource;

import java.time.Instant;

public record TimeEntryDto(
        Long id,
        Long taskId,
        TimeEntrySource source,
        Instant startedAt,
        Instant endedAt,
        Integer durationSeconds,
        String note
) {
}
