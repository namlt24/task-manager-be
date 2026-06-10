package com.taskmanager.report.dto;

import java.util.List;
import java.util.Map;

public record ReportSummaryDto(
        long total,
        long completed,
        long pending,
        long overdue,
        double completionRate,            // 0..100
        Map<String, Long> byPriority,     // LOW/MEDIUM/HIGH/URGENT -> count
        List<CategoryCountDto> byCategory,
        long trackedTodaySeconds,
        long trackedWeekSeconds,
        int streakDays                    // consecutive days with >=1 completed task
) {
}
