package com.taskmanager.timetracking.repository;

import com.taskmanager.timetracking.entity.TimeEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    Optional<TimeEntry> findByIdAndUserId(Long id, Long userId);

    /** The user's currently running entry, if any (ended_at IS NULL). */
    Optional<TimeEntry> findFirstByUserIdAndEndedAtIsNull(Long userId);

    List<TimeEntry> findByUserIdOrderByStartedAtDesc(Long userId);

    List<TimeEntry> findByUserIdAndTaskIdOrderByStartedAtDesc(Long userId, Long taskId);

    List<TimeEntry> findByUserIdAndStartedAtGreaterThanEqualOrderByStartedAtDesc(Long userId, Instant from);

    /** Total finished seconds logged against a single task. */
    @Query("select coalesce(sum(t.durationSeconds), 0) from TimeEntry t " +
            "where t.taskId = :taskId and t.durationSeconds is not null")
    long sumDurationByTaskId(@Param("taskId") Long taskId);

    /** Total finished seconds grouped by task, for a set of tasks (batch, avoids N+1). */
    @Query("select t.taskId, coalesce(sum(t.durationSeconds), 0) from TimeEntry t " +
            "where t.taskId in :taskIds and t.durationSeconds is not null group by t.taskId")
    List<Object[]> sumDurationByTaskIds(@Param("taskIds") List<Long> taskIds);
}
