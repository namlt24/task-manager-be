package com.taskmanager.task.repository;

import com.taskmanager.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long>, JpaSpecificationExecutor<Task> {

    List<Task> findByBoardIdOrderByColumnIdAscPositionAscIdAsc(Long boardId);

    List<Task> findByColumnIdOrderByPositionAscIdAsc(Long columnId);

    Optional<Task> findByIdAndWorkspaceId(Long id, Long workspaceId);

    long countByColumnId(Long columnId);

    /** System-wide query for the reminder scheduler (not scoped to a user/workspace). */
    List<Task> findByRemindAtLessThanEqualAndReminderSentFalseAndCompletedFalse(Instant now);

    /** Tasks with a due date in a range — for the calendar view (workspace-scoped). */
    List<Task> findByWorkspaceIdAndDueDateBetweenOrderByDueDateAsc(Long workspaceId, Instant from, Instant to);

    /** All of the workspace's tasks (CSV export). */
    List<Task> findByWorkspaceIdOrderByCreatedAtDesc(Long workspaceId);
}
