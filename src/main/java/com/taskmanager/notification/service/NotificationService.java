package com.taskmanager.notification.service;

import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.notification.dto.NotificationDto;
import com.taskmanager.notification.entity.Notification;
import com.taskmanager.notification.entity.NotificationType;
import com.taskmanager.notification.mapper.NotificationMapper;
import com.taskmanager.notification.repository.NotificationRepository;
import com.taskmanager.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository repository;
    private final NotificationMapper mapper;

    public NotificationService(NotificationRepository repository, NotificationMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    /** Creates a notification for a specific user — used by schedulers (no security context required). */
    @Transactional
    public Notification create(Long userId, NotificationType type, String title, String message, Long taskId) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setTaskId(taskId);
        n.setRead(false);
        return repository.save(n);
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> list(boolean unreadOnly) {
        Long userId = SecurityUtils.getCurrentUserId();
        List<Notification> items = unreadOnly
                ? repository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId)
                : repository.findByUserIdOrderByCreatedAtDesc(userId);
        return items.stream().map(mapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        return repository.countByUserIdAndReadFalse(SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public NotificationDto markRead(Long id) {
        Notification n = loadOwned(id);
        n.setRead(true);
        return mapper.toDto(repository.save(n));
    }

    @Transactional
    public void markAllRead() {
        repository.markAllRead(SecurityUtils.getCurrentUserId());
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(loadOwned(id));
    }

    private Notification loadOwned(Long id) {
        return repository.findByIdAndUserId(id, SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Notification", id));
    }
}
