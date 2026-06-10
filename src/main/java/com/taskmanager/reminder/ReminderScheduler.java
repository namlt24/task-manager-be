package com.taskmanager.reminder;

import com.taskmanager.messaging.EventPublisher;
import com.taskmanager.messaging.event.EventTypes;
import com.taskmanager.messaging.event.TaskEventPayload;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.user.entity.User;
import com.taskmanager.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Periodically scans tasks whose remind-at time has passed and fires an in-app notification + email,
 * marking each task as reminded so it is not notified again.
 */
@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);
    private static final DateTimeFormatter DUE_FMT =
            DateTimeFormatter.ofPattern("HH:mm 'ngày' dd/MM/yyyy", new Locale("vi"));

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;
    private final EventPublisher eventPublisher;

    public ReminderScheduler(TaskRepository taskRepository,
                             UserRepository userRepository,
                             EventPublisher eventPublisher) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Scans due reminders and publishes a TASK_REMINDER event (via the outbox, in the same transaction
     * as marking the task reminded) so the notification + email are delivered asynchronously by the
     * Kafka consumer. The recipient is the assignee when set, otherwise the task creator.
     */
    @Scheduled(fixedDelayString = "${app.scheduler.reminder-poll-ms:60000}")
    @Transactional
    public void scanDueReminders() {
        List<Task> due = taskRepository
                .findByRemindAtLessThanEqualAndReminderSentFalseAndCompletedFalse(Instant.now());
        if (due.isEmpty()) {
            return;
        }
        log.info("Reminder scan: {} task(s) due", due.size());
        for (Task task : due) {
            try {
                String dueText = formatDue(task);
                Long recipient = task.getAssigneeId() != null ? task.getAssigneeId() : task.getUserId();
                eventPublisher.publish(EventTypes.AGGREGATE_TASK, task.getId(), EventTypes.TASK_REMINDER,
                        new TaskEventPayload(EventTypes.TASK_REMINDER, task.getId(), task.getTitle(),
                                task.getWorkspaceId(), recipient, null, "Nhắc việc: " + task.getTitle(),
                                dueText != null ? "Hạn: " + dueText : "Công việc đã tới giờ nhắc.", dueText));
                task.setReminderSent(true);
            } catch (Exception ex) {
                // Never let one bad task break the whole batch; it will retry next scan if still unsent.
                log.error("Failed to process reminder for task {}: {}", task.getId(), ex.getMessage());
            }
        }
        taskRepository.saveAll(due);
    }

    private String formatDue(Task task) {
        if (task.getDueDate() == null) {
            return null;
        }
        ZoneId zone = ZoneId.of("UTC");
        User u = userRepository.findById(task.getUserId()).orElse(null);
        if (u != null && u.getTimezone() != null && !u.getTimezone().isBlank()) {
            try {
                zone = ZoneId.of(u.getTimezone());
            } catch (Exception ignored) {
                // fall back to UTC on invalid zone id
            }
        }
        return DUE_FMT.format(task.getDueDate().atZone(zone));
    }
}
