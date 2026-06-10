package com.taskmanager.category.service;

import com.taskmanager.category.dto.CategoryDto;
import com.taskmanager.category.dto.CategoryRequest;
import com.taskmanager.category.entity.Category;
import com.taskmanager.category.mapper.CategoryMapper;
import com.taskmanager.category.repository.CategoryRepository;
import com.taskmanager.common.exception.ConflictException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.workspace.WorkspaceAccess;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoryService {

    /** Cache key = current workspace id; evicted on any write so lists stay fresh. */
    private static final String KEY = "@workspaceAccess.currentWorkspaceId()";

    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final WorkspaceAccess access;

    public CategoryService(CategoryRepository repository, CategoryMapper mapper, WorkspaceAccess access) {
        this.repository = repository;
        this.mapper = mapper;
        this.access = access;
    }

    @Cacheable(cacheNames = "categories", key = KEY)
    @Transactional(readOnly = true)
    public List<CategoryDto> list() {
        Long workspaceId = access.currentWorkspaceId();
        return repository.findByWorkspaceIdOrderByPositionAscIdAsc(workspaceId).stream()
                .map(mapper::toDto)
                .toList();
    }

    @CacheEvict(cacheNames = "categories", key = KEY)
    @Transactional
    public CategoryDto create(CategoryRequest request) {
        Long workspaceId = access.requireManager();
        if (repository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, request.name())) {
            throw new ConflictException("Danh mục đã tồn tại");
        }
        Category category = new Category();
        category.setUserId(SecurityUtils.getCurrentUserId());
        category.setWorkspaceId(workspaceId);
        category.setName(request.name());
        category.setColor(request.color());
        category.setIcon(request.icon());
        category.setPosition((int) repository.countByWorkspaceId(workspaceId));
        return mapper.toDto(repository.save(category));
    }

    @CacheEvict(cacheNames = "categories", key = KEY)
    @Transactional
    public CategoryDto update(Long id, CategoryRequest request) {
        Long workspaceId = access.requireManager();
        Category category = repository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        if (!category.getName().equalsIgnoreCase(request.name())
                && repository.existsByWorkspaceIdAndNameIgnoreCase(workspaceId, request.name())) {
            throw new ConflictException("Danh mục đã tồn tại");
        }
        category.setName(request.name());
        category.setColor(request.color());
        category.setIcon(request.icon());
        return mapper.toDto(repository.save(category));
    }

    @CacheEvict(cacheNames = "categories", key = KEY)
    @Transactional
    public void delete(Long id) {
        Long workspaceId = access.requireManager();
        Category category = repository.findByIdAndWorkspaceId(id, workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        repository.delete(category);
    }
}
