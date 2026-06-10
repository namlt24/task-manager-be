package com.taskmanager.report.controller;

import com.taskmanager.report.dto.CalendarItemDto;
import com.taskmanager.report.dto.ProductivityDto;
import com.taskmanager.report.dto.ReportSummaryDto;
import com.taskmanager.report.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/v1/reports")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService service;

    public ReportController(ReportService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    @Operation(summary = "Overview stats (counts, completion rate, by priority/category, tracked time, streak)")
    public ReportSummaryDto summary() {
        return service.summary();
    }

    @GetMapping("/productivity")
    @Operation(summary = "Per-day created/completed/tracked over the last N days (default 14)")
    public ProductivityDto productivity(@RequestParam(required = false, defaultValue = "14") int days) {
        return service.productivity(days);
    }

    @GetMapping("/calendar")
    @Operation(summary = "Tasks with a due date in [from, to] for the calendar view")
    public List<CalendarItemDto> calendar(@RequestParam String from, @RequestParam String to) {
        return service.calendar(Instant.parse(from), Instant.parse(to));
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Export all tasks as CSV")
    public ResponseEntity<byte[]> export() {
        byte[] body = service.exportCsv().getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tasks-export.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(body);
    }
}
