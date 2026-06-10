package com.taskmanager.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.email.EmailService;
import com.taskmanager.messaging.event.EventTypes;
import com.taskmanager.messaging.event.TaskEventPayload;
import com.taskmanager.notification.entity.NotificationType;
import com.taskmanager.notification.service.NotificationService;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes task-events and delivers them: persists an in-app notification for the recipient and,
 * for assignments/reminders, sends an email. Runs off the request path so SMTP latency never blocks users.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class TaskEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TaskEventConsumer.class);

    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final EmailService emailService;

    public TaskEventConsumer(ObjectMapper objectMapper,
                             NotificationService notificationService,
                             UserRepository userRepository,
                             EmailService emailService) {
        this.objectMapper = objectMapper;
        this.notificationService = notificationService;
        this.userRepository = userRepository;
        this.emailService = emailService;
    }

    @KafkaListener(topics = "${app.messaging.topic:task-events}", groupId = "tm-notification")
    public void onTaskEvent(String message) {
        TaskEventPayload event;
        try {
            event = objectMapper.readValue(message, TaskEventPayload.class);
        } catch (Exception ex) {
            log.error("Skipping unparseable task-event: {}", ex.getMessage());
            return;
        }
        if (event.recipientUserId() == null) {
            return;
        }
        try {
            notificationService.create(event.recipientUserId(), mapType(event.type()),
                    event.title(), event.message(), event.taskId());

            User recipient = userRepository.findById(event.recipientUserId()).orElse(null);
            if (recipient != null) {
                sendEmail(event, recipient);
            }
            log.info("Delivered {} notification for task {} to user {}",
                    event.type(), event.taskId(), event.recipientUserId());
        } catch (Exception ex) {
            log.error("Failed to deliver task-event {} for task {}: {}",
                    event.type(), event.taskId(), ex.getMessage());
        }
    }

    private void sendEmail(TaskEventPayload event, User recipient) {
        String name = (recipient.getFullName() == null || recipient.getFullName().isBlank())
                ? recipient.getEmail() : recipient.getFullName();
        switch (event.type()) {
            case EventTypes.TASK_REMINDER ->
                    emailService.sendTaskReminder(recipient.getEmail(), name, event.taskTitle(), event.dueText());
            case EventTypes.TASK_ASSIGNED ->
                    emailService.sendTaskAssigned(recipient.getEmail(), name, event.taskTitle(), event.message());
            default -> { /* TASK_COMPLETED: in-app notification only */ }
        }
    }

    private NotificationType mapType(String type) {
        return switch (type) {
            case EventTypes.TASK_ASSIGNED -> NotificationType.ASSIGNED;
            case EventTypes.TASK_COMPLETED -> NotificationType.COMPLETED;
            default -> NotificationType.REMINDER;
        };
    }
}
