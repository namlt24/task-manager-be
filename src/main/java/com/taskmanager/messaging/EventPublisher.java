package com.taskmanager.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.taskmanager.messaging.entity.OutboxEvent;
import com.taskmanager.messaging.repository.OutboxEventRepository;
import org.springframework.stereotype.Component;

/**
 * Writes events into the outbox table. MUST be called inside the business transaction so the event is
 * committed atomically with the change that produced it; {@link OutboxRelay} ships it to Kafka afterwards.
 */
@Component
public class EventPublisher {

    private final OutboxEventRepository repository;
    private final ObjectMapper objectMapper;

    public EventPublisher(OutboxEventRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void publish(String aggregateType, Long aggregateId, String type, Object payload) {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType(aggregateType);
        event.setAggregateId(aggregateId);
        event.setType(type);
        event.setPayload(serialize(payload));
        repository.save(event);
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Cannot serialize event payload", ex);
        }
    }
}
