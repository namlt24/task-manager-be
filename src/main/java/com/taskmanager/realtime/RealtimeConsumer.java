package com.taskmanager.realtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.messaging.event.TaskEventPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

/**
 * Bridges Kafka task-events to STOMP: forwards each event to {@code /topic/workspace/{id}} so members
 * viewing that workspace get live updates (assignment, completion, …). Its own consumer group ensures
 * it receives every message independently of the notification/activity consumers.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class RealtimeConsumer {

    private static final Logger log = LoggerFactory.getLogger(RealtimeConsumer.class);

    private final ObjectMapper objectMapper;
    private final SimpMessagingTemplate messagingTemplate;

    public RealtimeConsumer(ObjectMapper objectMapper, SimpMessagingTemplate messagingTemplate) {
        this.objectMapper = objectMapper;
        this.messagingTemplate = messagingTemplate;
    }

    @KafkaListener(topics = "${app.messaging.topic:task-events}", groupId = "tm-realtime")
    public void onTaskEvent(String message) {
        TaskEventPayload event;
        try {
            event = objectMapper.readValue(message, TaskEventPayload.class);
        } catch (Exception ex) {
            log.error("Skipping unparseable realtime event: {}", ex.getMessage());
            return;
        }
        if (event.workspaceId() == null) {
            return;
        }
        messagingTemplate.convertAndSend("/topic/workspace/" + event.workspaceId(), event);
    }
}
