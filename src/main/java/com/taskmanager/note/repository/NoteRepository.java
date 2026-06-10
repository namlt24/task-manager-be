package com.taskmanager.note.repository;

import com.taskmanager.note.entity.Note;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    List<Note> findByWorkspaceIdOrderByUpdatedAtDesc(Long workspaceId);

    List<Note> findByWorkspaceIdAndTaskIdOrderByUpdatedAtDesc(Long workspaceId, Long taskId);

    Optional<Note> findByIdAndWorkspaceId(Long id, Long workspaceId);
}
