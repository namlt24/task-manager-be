package com.taskmanager.messaging.repository;

import com.taskmanager.messaging.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    List<OutboxEvent> findTop200ByPublishedFalseOrderByIdAsc();
}
