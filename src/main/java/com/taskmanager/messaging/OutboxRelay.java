package com.taskmanager.messaging;

import com.taskmanager.messaging.entity.OutboxEvent;
import com.taskmanager.messaging.repository.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Periodically ships unpublished outbox rows to Kafka, marking them published once the broker acks.
 * On a send failure (e.g. broker down) it stops the batch so nothing is lost — the rows stay unpublished
 * and are retried on the next tick.
 */
@Component
@ConditionalOnProperty(name = "app.messaging.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelay {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelay.class);

    private final OutboxEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;

    public OutboxRelay(OutboxEventRepository repository,
                       KafkaTemplate<String, String> kafkaTemplate,
                       @org.springframework.beans.factory.annotation.Value("${app.messaging.topic:task-events}") String topic) {
        this.repository = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
    }

    @Scheduled(fixedDelayString = "${app.messaging.outbox-poll-ms:5000}")
    @Transactional
    public void relay() {
        List<OutboxEvent> batch = repository.findTop200ByPublishedFalseOrderByIdAsc();
        if (batch.isEmpty()) {
            return;
        }
        int sent = 0;
        for (OutboxEvent event : batch) {
            try {
                String key = event.getAggregateId() != null ? event.getAggregateId().toString() : null;
                kafkaTemplate.send(topic, key, event.getPayload()).get();
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
                sent++;
            } catch (Exception ex) {
                // Broker hiccup: stop here, keep the rest unpublished, retry next tick.
                log.warn("Outbox relay stopped at event {}: {}", event.getId(), ex.getMessage());
                break;
            }
        }
        if (sent > 0) {
            repository.saveAll(batch);
            log.info("Outbox relay published {} event(s) to '{}'", sent, topic);
        }
    }
}
