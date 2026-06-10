package com.taskmanager.timetracking.controller;

import com.taskmanager.timetracking.dto.StartTimeEntryRequest;
import com.taskmanager.timetracking.dto.TimeEntryDto;
import com.taskmanager.timetracking.service.TimeEntryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/v1/time-entries")
@Tag(name = "Time tracking")
public class TimeEntryController {

    private final TimeEntryService service;

    public TimeEntryController(TimeEntryService service) {
        this.service = service;
    }

    @PostMapping("/start")
    @Operation(summary = "Start a timer session (stops any running one first)")
    public ResponseEntity<TimeEntryDto> start(@RequestBody(required = false) StartTimeEntryRequest request) {
        StartTimeEntryRequest req = request != null ? request : new StartTimeEntryRequest(null, null, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(service.start(req));
    }

    @PatchMapping("/{id}/stop")
    @Operation(summary = "Stop a running timer session")
    public TimeEntryDto stop(@PathVariable Long id) {
        return service.stop(id);
    }

    @GetMapping("/active")
    @Operation(summary = "Get the currently running session (or empty)")
    public ResponseEntity<TimeEntryDto> active() {
        TimeEntryDto active = service.getActive();
        return active != null ? ResponseEntity.ok(active) : ResponseEntity.noContent().build();
    }

    @GetMapping
    @Operation(summary = "List sessions (optional ?taskId= or ?from=ISO)")
    public List<TimeEntryDto> list(@RequestParam(required = false) Long taskId,
                                   @RequestParam(required = false) String from) {
        Instant fromInstant = (from != null && !from.isBlank()) ? Instant.parse(from) : null;
        return service.list(taskId, fromInstant);
    }

    @GetMapping("/summary")
    @Operation(summary = "Total tracked seconds for a task")
    public Map<String, Long> summary(@RequestParam Long taskId) {
        return Map.of("totalSeconds", service.totalSecondsForTask(taskId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a session")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
