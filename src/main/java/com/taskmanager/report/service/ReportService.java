package com.taskmanager.report.service;

import com.taskmanager.category.entity.Category;
import com.taskmanager.category.repository.CategoryRepository;
import com.taskmanager.label.entity.Label;
import com.taskmanager.report.dto.CalendarItemDto;
import com.taskmanager.report.dto.CategoryCountDto;
import com.taskmanager.report.dto.DayStatDto;
import com.taskmanager.report.dto.ProductivityDto;
import com.taskmanager.report.dto.ReportSummaryDto;
import com.taskmanager.report.repository.ReportRepository;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.timetracking.repository.TimeEntryRepository;
import com.taskmanager.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ReportService {

    private final ReportRepository reportRepository;
    private final TaskRepository taskRepository;
    private final CategoryRepository categoryRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final UserRepository userRepository;
    private final com.taskmanager.workspace.WorkspaceAccess access;

    public ReportService(ReportRepository reportRepository,
                         TaskRepository taskRepository,
                         CategoryRepository categoryRepository,
                         TimeEntryRepository timeEntryRepository,
                         UserRepository userRepository,
                         com.taskmanager.workspace.WorkspaceAccess access) {
        this.reportRepository = reportRepository;
        this.taskRepository = taskRepository;
        this.categoryRepository = categoryRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.userRepository = userRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public ReportSummaryDto summary() {
        Long ws = access.currentWorkspaceId();
        ZoneId zone = zoneOf(SecurityUtils.getCurrentUserId());

        long total = reportRepository.countTotal(ws);
        long completed = reportRepository.countCompleted(ws);
        long pending = total - completed;
        long overdue = reportRepository.countOverdue(ws);
        double rate = total == 0 ? 0 : Math.round((completed * 10000.0 / total)) / 100.0;

        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (String p : List.of("LOW", "MEDIUM", "HIGH", "URGENT")) {
            byPriority.put(p, 0L);
        }
        for (Object[] row : reportRepository.countByPriority(ws)) {
            byPriority.put((String) row[0], ((Number) row[1]).longValue());
        }

        List<CategoryCountDto> byCategory = new ArrayList<>();
        for (Object[] row : reportRepository.countByCategory(ws)) {
            byCategory.add(new CategoryCountDto(
                    ((Number) row[0]).longValue(),
                    (String) row[1],
                    (String) row[2],
                    ((Number) row[3]).longValue()));
        }

        Instant startToday = LocalDate.now(zone).atStartOfDay(zone).toInstant();
        Instant startWeek = LocalDate.now(zone).minusDays(6).atStartOfDay(zone).toInstant();
        long trackedToday = reportRepository.trackedSecondsSince(ws, startToday);
        long trackedWeek = reportRepository.trackedSecondsSince(ws, startWeek);

        int streak = computeStreak(reportRepository.completedDaysDesc(ws, zone.getId()), zone);

        return new ReportSummaryDto(total, completed, pending, overdue, rate,
                byPriority, byCategory, trackedToday, trackedWeek, streak);
    }

    @Transactional(readOnly = true)
    public ProductivityDto productivity(int days) {
        Long ws = access.currentWorkspaceId();
        ZoneId zone = zoneOf(SecurityUtils.getCurrentUserId());
        int n = Math.max(1, Math.min(days, 90));
        LocalDate today = LocalDate.now(zone);
        Instant from = today.minusDays(n - 1L).atStartOfDay(zone).toInstant();
        String tz = zone.getId();

        Map<String, Long> created = toMap(reportRepository.createdPerDay(ws, from, tz));
        Map<String, Long> completed = toMap(reportRepository.completedPerDay(ws, from, tz));
        Map<String, Long> tracked = toMap(reportRepository.trackedPerDay(ws, from, tz));

        List<DayStatDto> list = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            String key = today.minusDays(i).toString();
            list.add(new DayStatDto(key,
                    created.getOrDefault(key, 0L),
                    completed.getOrDefault(key, 0L),
                    tracked.getOrDefault(key, 0L)));
        }
        return new ProductivityDto(list);
    }

    @Transactional(readOnly = true)
    public List<CalendarItemDto> calendar(Instant from, Instant to) {
        Long ws = access.currentWorkspaceId();
        Map<Long, String> colors = new HashMap<>();
        for (Category c : categoryRepository.findByWorkspaceIdOrderByPositionAscIdAsc(ws)) {
            colors.put(c.getId(), c.getColor());
        }
        return taskRepository.findByWorkspaceIdAndDueDateBetweenOrderByDueDateAsc(ws, from, to).stream()
                .map(t -> new CalendarItemDto(t.getId(), t.getTitle(), t.getDueDate(), t.isCompleted(),
                        t.getPriority().name(),
                        t.getCategoryId() != null ? colors.get(t.getCategoryId()) : null))
                .toList();
    }

    @Transactional(readOnly = true)
    public String exportCsv() {
        Long ws = access.currentWorkspaceId();
        ZoneId zone = zoneOf(SecurityUtils.getCurrentUserId());
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);

        Map<Long, String> cats = new HashMap<>();
        for (Category c : categoryRepository.findByWorkspaceIdOrderByPositionAscIdAsc(ws)) {
            cats.put(c.getId(), c.getName());
        }
        List<Task> tasks = taskRepository.findByWorkspaceIdOrderByCreatedAtDesc(ws);
        Map<Long, Long> tracked = new HashMap<>();
        List<Long> ids = tasks.stream().map(Task::getId).toList();
        if (!ids.isEmpty()) {
            for (Object[] row : timeEntryRepository.sumDurationByTaskIds(ids)) {
                tracked.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM so Excel detects UTF-8
        sb.append("ID,Tiêu đề,Ưu tiên,Trạng thái,Hạn,Nhắc,Tạo lúc,Hoàn thành lúc,Danh mục,Nhãn,Thời gian (giây)\n");
        for (Task t : tasks) {
            String labels = String.join(" | ", t.getLabels().stream().map(Label::getName).toList());
            sb.append(t.getId()).append(',')
                    .append(csv(t.getTitle())).append(',')
                    .append(t.getPriority().name()).append(',')
                    .append(t.isCompleted() ? "Hoàn thành" : "Chưa xong").append(',')
                    .append(t.getDueDate() != null ? fmt.format(t.getDueDate()) : "").append(',')
                    .append(t.getRemindAt() != null ? fmt.format(t.getRemindAt()) : "").append(',')
                    .append(t.getCreatedAt() != null ? fmt.format(t.getCreatedAt()) : "").append(',')
                    .append(t.getCompletedAt() != null ? fmt.format(t.getCompletedAt()) : "").append(',')
                    .append(csv(t.getCategoryId() != null ? cats.getOrDefault(t.getCategoryId(), "") : "")).append(',')
                    .append(csv(labels)).append(',')
                    .append(tracked.getOrDefault(t.getId(), 0L))
                    .append('\n');
        }
        return sb.toString();
    }

    // ---- helpers ----
    private ZoneId zoneOf(Long uid) {
        return userRepository.findById(uid)
                .map(u -> {
                    try {
                        return (u.getTimezone() != null && !u.getTimezone().isBlank())
                                ? ZoneId.of(u.getTimezone()) : ZoneId.of("UTC");
                    } catch (Exception e) {
                        return ZoneId.of("UTC");
                    }
                })
                .orElse(ZoneId.of("UTC"));
    }

    private Map<String, Long> toMap(List<Object[]> rows) {
        Map<String, Long> m = new HashMap<>();
        for (Object[] row : rows) {
            m.put((String) row[0], ((Number) row[1]).longValue());
        }
        return m;
    }

    private int computeStreak(List<String> daysDesc, ZoneId zone) {
        Set<LocalDate> set = new HashSet<>();
        for (String d : daysDesc) {
            set.add(LocalDate.parse(d));
        }
        LocalDate cursor = LocalDate.now(zone);
        if (!set.contains(cursor)) {
            cursor = cursor.minusDays(1); // allow a streak that is current up to yesterday
        }
        int streak = 0;
        while (set.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return '"' + value.replace("\"", "\"\"") + '"';
        }
        return value;
    }
}
