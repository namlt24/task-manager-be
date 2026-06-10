package com.taskmanager.activity.repository;

import com.taskmanager.activity.entity.ActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    List<ActivityLog> findTop50ByWorkspaceIdOrderByIdDesc(Long workspaceId);
}
