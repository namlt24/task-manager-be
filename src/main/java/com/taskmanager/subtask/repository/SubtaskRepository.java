package com.taskmanager.subtask.repository;

import com.taskmanager.subtask.entity.Subtask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SubtaskRepository extends JpaRepository<Subtask, Long> {

    List<Subtask> findByTaskIdOrderByPositionAscIdAsc(Long taskId);

    List<Subtask> findByTaskIdInOrderByPositionAscIdAsc(Collection<Long> taskIds);

    Optional<Subtask> findByIdAndUserId(Long id, Long userId);

    long countByTaskId(Long taskId);

    long countByTaskIdAndCompletedTrue(Long taskId);
}
