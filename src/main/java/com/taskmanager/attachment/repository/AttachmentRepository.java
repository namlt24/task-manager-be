package com.taskmanager.attachment.repository;

import com.taskmanager.attachment.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    List<Attachment> findByTaskIdOrderByCreatedAtDesc(Long taskId);

    List<Attachment> findByTaskIdIn(Collection<Long> taskIds);

    Optional<Attachment> findByIdAndUserId(Long id, Long userId);

    long countByTaskId(Long taskId);
}
