package com.taskmanager.task.dto;

/** Assigns a task to a workspace member. {@code assigneeId == null} unassigns the task. */
public record AssignTaskRequest(
        Long assigneeId
) {
}
