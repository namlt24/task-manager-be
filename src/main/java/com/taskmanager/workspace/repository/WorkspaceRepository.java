package com.taskmanager.workspace.repository;

import com.taskmanager.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    Optional<Workspace> findFirstByOwnerIdOrderByIdAsc(Long ownerId);
}
