package com.taskmanager.messaging.event;

/**
 * The payload carried on the {@code task-events} topic. Built display-ready at publish time so the
 * consumer only persists a notification and (optionally) sends an email — no extra business lookups.
 */
public record TaskEventPayload(
        String type,             // TASK_ASSIGNED / TASK_COMPLETED / TASK_REMINDER (see EventTypes)
        Long taskId,
        String taskTitle,
        Long workspaceId,
        Long recipientUserId,    // who gets notified (assignee/creator); also the activity target
        Long actorId,            // who performed the action (null for system reminders)
        String title,            // notification title
        String message,          // notification body
        String dueText           // reminders only (nullable)
) {
}
