package com.taskmanager.activity.controller;

import com.taskmanager.activity.dto.ActivityDto;
import com.taskmanager.activity.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/workspaces/{id}/activity")
@Tag(name = "Activity")
public class ActivityController {

    private final ActivityService service;

    public ActivityController(ActivityService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List recent activity of a workspace (members only)")
    public List<ActivityDto> list(@PathVariable Long id) {
        return service.list(id);
    }
}
