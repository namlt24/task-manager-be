package com.taskmanager.report.repository;

import com.taskmanager.task.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * Aggregate (mostly native) queries for the reporting dashboard. All scoped to a workspace id.
 * Day-grouping uses {@code AT TIME ZONE :tz} so buckets align with the user's local day.
 * Tracked-time queries join time_entries → tasks so they reflect the whole workspace.
 */
public interface ReportRepository extends JpaRepository<Task, Long> {

    @Query(value = "SELECT count(*) FROM tasks WHERE workspace_id = :ws", nativeQuery = true)
    long countTotal(@Param("ws") Long ws);

    @Query(value = "SELECT count(*) FROM tasks WHERE workspace_id = :ws AND completed = true", nativeQuery = true)
    long countCompleted(@Param("ws") Long ws);

    @Query(value = "SELECT count(*) FROM tasks WHERE workspace_id = :ws AND completed = false " +
            "AND due_date IS NOT NULL AND due_date < now()", nativeQuery = true)
    long countOverdue(@Param("ws") Long ws);

    @Query(value = "SELECT priority, count(*) FROM tasks WHERE workspace_id = :ws GROUP BY priority", nativeQuery = true)
    List<Object[]> countByPriority(@Param("ws") Long ws);

    @Query(value = "SELECT c.id, c.name, c.color, count(t.id) " +
            "FROM categories c LEFT JOIN tasks t ON t.category_id = c.id AND t.workspace_id = c.workspace_id " +
            "WHERE c.workspace_id = :ws GROUP BY c.id, c.name, c.color ORDER BY count(t.id) DESC", nativeQuery = true)
    List<Object[]> countByCategory(@Param("ws") Long ws);

    @Query(value = "SELECT coalesce(sum(te.duration_seconds), 0) FROM time_entries te " +
            "JOIN tasks t ON te.task_id = t.id " +
            "WHERE t.workspace_id = :ws AND te.duration_seconds IS NOT NULL AND te.started_at >= :from",
            nativeQuery = true)
    long trackedSecondsSince(@Param("ws") Long ws, @Param("from") Instant from);

    // ---- per-day series (last N days) ----
    @Query(value = "SELECT to_char(date_trunc('day', created_at AT TIME ZONE :tz), 'YYYY-MM-DD') d, count(*) " +
            "FROM tasks WHERE workspace_id = :ws AND created_at >= :from GROUP BY d", nativeQuery = true)
    List<Object[]> createdPerDay(@Param("ws") Long ws, @Param("from") Instant from, @Param("tz") String tz);

    @Query(value = "SELECT to_char(date_trunc('day', completed_at AT TIME ZONE :tz), 'YYYY-MM-DD') d, count(*) " +
            "FROM tasks WHERE workspace_id = :ws AND completed_at IS NOT NULL AND completed_at >= :from GROUP BY d",
            nativeQuery = true)
    List<Object[]> completedPerDay(@Param("ws") Long ws, @Param("from") Instant from, @Param("tz") String tz);

    @Query(value = "SELECT to_char(date_trunc('day', te.started_at AT TIME ZONE :tz), 'YYYY-MM-DD') d, " +
            "coalesce(sum(te.duration_seconds), 0) FROM time_entries te JOIN tasks t ON te.task_id = t.id " +
            "WHERE t.workspace_id = :ws AND te.duration_seconds IS NOT NULL AND te.started_at >= :from GROUP BY d",
            nativeQuery = true)
    List<Object[]> trackedPerDay(@Param("ws") Long ws, @Param("from") Instant from, @Param("tz") String tz);

    // ---- streak: distinct local days that have at least one completed task, newest first ----
    @Query(value = "SELECT DISTINCT to_char(date_trunc('day', completed_at AT TIME ZONE :tz), 'YYYY-MM-DD') d " +
            "FROM tasks WHERE workspace_id = :ws AND completed_at IS NOT NULL ORDER BY d DESC", nativeQuery = true)
    List<String> completedDaysDesc(@Param("ws") Long ws, @Param("tz") String tz);
}
