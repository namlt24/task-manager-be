package com.taskmanager.timetracking.dto;

import com.taskmanager.timetracking.entity.TimeEntrySource;

public record StartTimeEntryRequest(
        Long taskId,                 // optional: focus without a task
        TimeEntrySource source,      // optional: defaults to STOPWATCH
        String note
) {
}
