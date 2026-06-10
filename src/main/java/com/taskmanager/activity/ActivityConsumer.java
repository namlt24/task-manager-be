package com.taskmanager.activity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.activity.service.ActivityService;
import com.taskmanager.messaging.event.EventTypes;
import com.taskmanager.messaging.event.TaskEventPayload;
import com.taskmanager.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes task-events and records workspace activity (who did what). System events without an actor
 * (e.g. reminders) are skipped. Uses its own consumer group so it sees every message independently
 * of the notification/realtime consumers.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class ActivityConsumer {

    private static final Logger log = LoggerFactory.getLogger(ActivityConsumer.class);

    private final ObjectMapper objectMapper;
    private final ActivityService activityService;
    private final UserRepository userRepository;

    public ActivityConsumer(ObjectMapper objectMapper, ActivityService activityService,
                            UserRepository userRepository) {
        this.objectMapper = objectMapper;
        this.activityService = activityService;
        this.userRepository = userRepository;
    }

    @KafkaListener(topics = "${app.messaging.topic:task-events}", groupId = "tm-activity")
    public void onTaskEvent(String message) {
        TaskEventPayload event;
        try {
            event = objectMapper.readValue(message, TaskEventPayload.class);
        } catch (Exception ex) {
            log.error("Skipping unparseable activity event: {}", ex.getMessage());
            return;
        }
        if (event.actorId() == null || event.workspaceId() == null) {
            return; // system event (e.g. reminder) — not a user action
        }
        String text = buildMessage(event);
        if (text == null) {
            return;
        }
        try {
            activityService.record(event.workspaceId(), event.actorId(), event.type(),
                    event.taskId(), event.recipientUserId(), text);
        } catch (Exception ex) {
            log.error("Failed to record activity for task {}: {}", event.taskId(), ex.getMessage());
        }
    }

    private String buildMessage(TaskEventPayload event) {
        String actor = nameOf(event.actorId());
        return switch (event.type()) {
            case EventTypes.TASK_ASSIGNED ->
                    actor + " đã giao việc «" + event.taskTitle() + "» cho " + nameOf(event.recipientUserId());
            case EventTypes.TASK_COMPLETED ->
                    actor + " đã hoàn thành «" + event.taskTitle() + "»";
            default -> null;
        };
    }

    private String nameOf(Long userId) {
        if (userId == null) {
            return "Ai đó";
        }
        return userRepository.findById(userId)
                .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName() : u.getEmail())
                .orElse("Ai đó");
    }
}
