package com.taskmanager.messaging.event;

/** Event type constants used on the task-events topic and in the outbox. */
public final class EventTypes {

    public static final String TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String TASK_COMPLETED = "TASK_COMPLETED";
    public static final String TASK_REMINDER = "TASK_REMINDER";

    public static final String AGGREGATE_TASK = "TASK";

    private EventTypes() {
    }
}
