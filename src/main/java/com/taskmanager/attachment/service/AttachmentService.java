package com.taskmanager.attachment.service;

import com.taskmanager.attachment.dto.AttachmentDto;
import com.taskmanager.attachment.entity.Attachment;
import com.taskmanager.attachment.repository.AttachmentRepository;
import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
public class AttachmentService {

    public record DownloadResource(Resource resource, String filename, String contentType) {
    }

    private final AttachmentRepository repository;
    private final FileStorageService storage;
    private final TaskRepository taskRepository;
    private final WorkspaceAccess access;

    public AttachmentService(AttachmentRepository repository, FileStorageService storage,
                             TaskRepository taskRepository, WorkspaceAccess access) {
        this.repository = repository;
        this.storage = storage;
        this.taskRepository = taskRepository;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<AttachmentDto> list(Long taskId) {
        requireOwnedTask(taskId);
        return repository.findByTaskIdOrderByCreatedAtDesc(taskId).stream().map(this::toDto).toList();
    }

    @Transactional
    public AttachmentDto upload(Long taskId, MultipartFile file) {
        requireOwnedTask(taskId);
        Long userId = SecurityUtils.getCurrentUserId();
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Tệp rỗng");
        }
        String original = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String storedName = UUID.randomUUID() + extension(original);
        storage.store(file, storedName);

        Attachment a = new Attachment();
        a.setTaskId(taskId);
        a.setUserId(userId);
        a.setOriginalName(original);
        a.setStoredName(storedName);
        a.setContentType(file.getContentType());
        a.setSizeBytes(file.getSize());
        return toDto(repository.save(a));
    }

    @Transactional(readOnly = true)
    public DownloadResource loadForDownload(Long id) {
        Attachment a = loadOwned(id);
        Resource resource = storage.loadAsResource(a.getStoredName());
        String ct = a.getContentType() != null ? a.getContentType() : "application/octet-stream";
        return new DownloadResource(resource, a.getOriginalName(), ct);
    }

    @Transactional
    public void delete(Long id) {
        Attachment a = loadOwned(id);
        repository.delete(a);
        storage.delete(a.getStoredName());
    }

    private Attachment loadOwned(Long id) {
        Attachment a = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", id));
        requireOwnedTask(a.getTaskId());
        return a;
    }

    /** Ensures the task exists in the current workspace (caller is a member). */
    private void requireOwnedTask(Long taskId) {
        taskRepository.findByIdAndWorkspaceId(taskId, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }

    private AttachmentDto toDto(Attachment a) {
        return new AttachmentDto(a.getId(), a.getTaskId(), a.getOriginalName(),
                a.getContentType(), a.getSizeBytes(), a.getCreatedAt());
    }

    private String extension(String name) {
        int dot = name.lastIndexOf('.');
        return (dot >= 0 && dot < name.length() - 1) ? name.substring(dot) : "";
    }
}
