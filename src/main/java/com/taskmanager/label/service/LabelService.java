package com.taskmanager.label.service;

import com.taskmanager.common.exception.ConflictException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.label.dto.LabelDto;
import com.taskmanager.label.dto.LabelRequest;
import com.taskmanager.label.entity.Label;
import com.taskmanager.label.mapper.LabelMapper;
import com.taskmanager.label.repository.LabelRepository;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LabelService {

    private static final String KEY = "@workspaceAccess.currentWorkspaceId()";

    private final LabelRepository repository;
    private final LabelMapper mapper;
    private final WorkspaceAccess access;

    public LabelService(LabelRepository repository, LabelMapper mapper, WorkspaceAccess access) {
        this.repository = repository;
        this.mapper = mapper;
        this.access = access;
    }

    @Cacheable(cacheNames = "labels", key = KEY)
    @Transactional(readOnly = true)
    public List<LabelDto> list() {
        return repository.findByWorkspaceIdOrderByNameAsc(access.currentWorkspaceId()).stream()
                .map(mapper::toDto)
                .toList();
    }

    @CacheEvict(cacheNames = "labels", key = KEY)
    @Transactional
    public LabelDto create(LabelRequest request) {
        Long workspaceId = access.requireManager();
        if (repository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, request.name())) {
            throw new ConflictException("Nhãn đã tồn tại");
        }
        Label label = new Label();
        label.setUserId(SecurityUtils.getCurrentUserId());
        label.setWorkspaceId(workspaceId);
        label.setName(request.name());
        label.setColor(request.color());
        return mapper.toDto(repository.save(label));
    }

    @CacheEvict(cacheNames = "labels", key = KEY)
    @Transactional
    public LabelDto update(Long id, LabelRequest request) {
        Long workspaceId = access.requireManager();
        Label label = repository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", id));
        if (!label.getName().equalsIgnoreCase(request.name())
                && repository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, request.name())) {
            throw new ConflictException("Nhãn đã tồn tại");
        }
        label.setName(request.name());
        label.setColor(request.color());
        return mapper.toDto(repository.save(label));
    }

    @CacheEvict(cacheNames = "labels", key = KEY)
    @Transactional
    public void delete(Long id) {
        Long workspaceId = access.requireManager();
        Label label = repository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Label", id));
        repository.delete(label);
    }
}
