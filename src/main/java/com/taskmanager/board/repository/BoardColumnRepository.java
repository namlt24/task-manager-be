package com.taskmanager.board.repository;

import com.taskmanager.board.entity.BoardColumn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardColumnRepository extends JpaRepository<BoardColumn, Long> {

    List<BoardColumn> findByBoardIdOrderByPositionAscIdAsc(Long boardId);

    Optional<BoardColumn> findByIdAndWorkspaceId(Long id, Long workspaceId);

    long countByBoardId(Long boardId);
}
