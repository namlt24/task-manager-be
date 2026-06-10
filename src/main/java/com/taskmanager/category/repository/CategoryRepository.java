package com.taskmanager.category.repository;

import com.taskmanager.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByWorkspaceIdOrderByPositionAscIdAsc(Long workspaceId);

    Optional<Category> findByIdAndWorkspaceId(Long id, Long workspaceId);

    boolean existsByWorkspaceIdAndNameIgnoreCase(Long workspaceId, String name);

    long countByWorkspaceId(Long workspaceId);
}
