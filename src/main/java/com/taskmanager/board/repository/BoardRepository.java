package com.taskmanager.board.repository;

import com.taskmanager.board.entity.Board;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BoardRepository extends JpaRepository<Board, Long> {

    List<Board> findByWorkspaceIdOrderByPositionAscIdAsc(Long workspaceId);

    Optional<Board> findByIdAndWorkspaceId(Long id, Long workspaceId);

    long countByWorkspaceId(Long workspaceId);
}
