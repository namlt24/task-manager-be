package com.taskmanager.task.service;

import com.taskmanager.board.entity.BoardColumn;
import com.taskmanager.board.repository.BoardColumnRepository;
import com.taskmanager.board.repository.BoardRepository;
import com.taskmanager.category.repository.CategoryRepository;
import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.label.entity.Label;
import com.taskmanager.label.repository.LabelRepository;
import com.taskmanager.attachment.repository.AttachmentRepository;
import com.taskmanager.messaging.EventPublisher;
import com.taskmanager.messaging.event.EventTypes;
import com.taskmanager.messaging.event.TaskEventPayload;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.subtask.repository.SubtaskRepository;
import com.taskmanager.common.exception.ForbiddenException;
import com.taskmanager.task.dto.AssignTaskRequest;
import com.taskmanager.task.dto.CreateTaskRequest;
import com.taskmanager.task.dto.MoveTaskRequest;
import com.taskmanager.task.dto.TaskDto;
import com.taskmanager.task.dto.UpdateTaskRequest;
import com.taskmanager.task.entity.Priority;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.mapper.TaskMapper;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.timetracking.repository.TimeEntryRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TaskService {

    private final TaskRepository taskRepository;
    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final CategoryRepository categoryRepository;
    private final LabelRepository labelRepository;
    private final SubtaskRepository subtaskRepository;
    private final AttachmentRepository attachmentRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final WorkspaceAccess access;
    private final EventPublisher eventPublisher;
    private final TaskMapper mapper;

    public TaskService(TaskRepository taskRepository,
                       BoardRepository boardRepository,
                       BoardColumnRepository columnRepository,
                       CategoryRepository categoryRepository,
                       LabelRepository labelRepository,
                       SubtaskRepository subtaskRepository,
                       AttachmentRepository attachmentRepository,
                       TimeEntryRepository timeEntryRepository,
                       WorkspaceAccess access,
                       EventPublisher eventPublisher,
                       TaskMapper mapper) {
        this.taskRepository = taskRepository;
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.categoryRepository = categoryRepository;
        this.labelRepository = labelRepository;
        this.subtaskRepository = subtaskRepository;
        this.attachmentRepository = attachmentRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.access = access;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
    }

    private TaskDto dto(Task task) {
        int total = (int) subtaskRepository.countByTaskId(task.getId());
        int done = (int) subtaskRepository.countByTaskIdAndCompletedTrue(task.getId());
        int attachments = (int) attachmentRepository.countByTaskId(task.getId());
        long tracked = timeEntryRepository.sumDurationByTaskId(task.getId());
        return mapper.toDto(task, total, done, attachments, tracked);
    }

    @Transactional(readOnly = true)
    public TaskDto get(Long id) {
        return dto(loadOwned(id));
    }

    /** Advanced search/filter over the current user's tasks (JPA Specification). All filters optional. */
    @Transactional(readOnly = true)
    public List<TaskDto> search(String q, Long boardId, Long categoryId, Long labelId,
                                Priority priority, Boolean completed, Instant dueFrom, Instant dueTo,
                                Long assigneeId) {
        Long workspaceId = access.currentWorkspaceId();
        Specification<Task> spec = (root, query, cb) -> {
            query.distinct(true);
            List<Predicate> ps = new ArrayList<>();
            ps.add(cb.equal(root.get("workspaceId"), workspaceId));
            if (boardId != null) ps.add(cb.equal(root.get("boardId"), boardId));
            if (categoryId != null) ps.add(cb.equal(root.get("categoryId"), categoryId));
            if (assigneeId != null) ps.add(cb.equal(root.get("assigneeId"), assigneeId));
            if (priority != null) ps.add(cb.equal(root.get("priority"), priority));
            if (completed != null) ps.add(cb.equal(root.get("completed"), completed));
            if (dueFrom != null) ps.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueFrom));
            if (dueTo != null) ps.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueTo));
            if (q != null && !q.isBlank()) {
                String like = "%" + q.toLowerCase() + "%";
                ps.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like)
                ));
            }
            if (labelId != null) {
                Join<Object, Object> labels = root.join("labels");
                ps.add(cb.equal(labels.get("id"), labelId));
            }
            return cb.and(ps.toArray(new Predicate[0]));
        };
        return taskRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream().map(this::dto).toList();
    }

    @Transactional
    public TaskDto create(CreateTaskRequest req) {
        Long userId = SecurityUtils.getCurrentUserId();
        Long workspaceId = access.currentWorkspaceId();
        boardRepository.findByIdAndWorkspaceId(req.boardId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Board", req.boardId()));
        BoardColumn column = columnRepository.findByIdAndWorkspaceId(req.columnId(), workspaceId)
                .orElseThrow(() -> new ResourceNotFoundException("Column", req.columnId()));
        if (!column.getBoardId().equals(req.boardId())) {
            throw new BadRequestException("Cột không thuộc bảng đã chọn");
        }

        Task task = new Task();
        task.setUserId(userId);
        task.setWorkspaceId(workspaceId);
        task.setBoardId(req.boardId());
        task.setColumnId(req.columnId());
        task.setTitle(req.title());
        task.setDescription(req.description());
        task.setPriority(req.priority() != null ? req.priority() : Priority.MEDIUM);
        task.setDueDate(req.dueDate());
        task.setRemindAt(req.remindAt());
        task.setRecurrenceFreq(req.recurrenceFreq());
        task.setRecurrenceInterval(req.recurrenceInterval());
        task.setRecurrenceUntil(req.recurrenceUntil());
        task.setCategoryId(resolveCategory(req.categoryId(), workspaceId));
        task.setAssigneeId(resolveAssigneeOnCreate(req.assigneeId(), userId, workspaceId));
        task.setLabels(resolveLabels(req.labelIds(), workspaceId));
        task.setPosition((int) taskRepository.countByColumnId(req.columnId()));
        return dto(taskRepository.save(task));
    }

    /**
     * Assigns/unassigns a task to a workspace member. Requires MANAGER/OWNER; the assignee must be a
     * member of the current workspace. {@code assigneeId == null} clears the assignment.
     */
    @Transactional
    public TaskDto assign(Long id, AssignTaskRequest req) {
        access.requireManager();
        Task task = loadOwned(id);
        Long assigneeId = req.assigneeId();
        if (assigneeId != null && !access.isMember(task.getWorkspaceId(), assigneeId)) {
            throw new BadRequestException("Người được giao không thuộc workspace");
        }
        task.setAssigneeId(assigneeId);
        Task saved = taskRepository.save(task);

        // Notify the new assignee (unless they assigned the task to themselves).
        Long actor = SecurityUtils.getCurrentUserId();
        if (assigneeId != null && !assigneeId.equals(actor)) {
            eventPublisher.publish(EventTypes.AGGREGATE_TASK, saved.getId(), EventTypes.TASK_ASSIGNED,
                    new TaskEventPayload(EventTypes.TASK_ASSIGNED, saved.getId(), saved.getTitle(),
                            saved.getWorkspaceId(), assigneeId, actor, "Bạn được giao việc",
                            "Bạn được giao việc: " + saved.getTitle(), null));
        }
        return dto(saved);
    }

    /**
     * At create time, a member may only assign the task to themselves; assigning to someone else
     * requires MANAGER/OWNER. The assignee (when set) must belong to the workspace.
     */
    private Long resolveAssigneeOnCreate(Long assigneeId, Long currentUserId, Long workspaceId) {
        if (assigneeId == null) {
            return null;
        }
        if (!assigneeId.equals(currentUserId)
                && access.roleOf(workspaceId, currentUserId) == com.taskmanager.workspace.entity.WorkspaceRole.MEMBER) {
            throw new ForbiddenException("Chỉ quản lý mới được giao việc cho người khác");
        }
        if (!access.isMember(workspaceId, assigneeId)) {
            throw new BadRequestException("Người được giao không thuộc workspace");
        }
        return assigneeId;
    }

    @Transactional
    public TaskDto update(Long id, UpdateTaskRequest req) {
        Task task = loadOwned(id);
        boolean wasCompleted = task.isCompleted();
        Instant oldRemindAt = task.getRemindAt();

        task.setTitle(req.title());
        task.setDescription(req.description());
        if (req.priority() != null) {
            task.setPriority(req.priority());
        }
        task.setDueDate(req.dueDate());
        task.setRemindAt(req.remindAt());
        // Re-arm the reminder if the time changed (or was cleared) so the scheduler fires again.
        if (!java.util.Objects.equals(oldRemindAt, req.remindAt())) {
            task.setReminderSent(false);
        }
        task.setRecurrenceFreq(req.recurrenceFreq());
        task.setRecurrenceInterval(req.recurrenceInterval());
        task.setRecurrenceUntil(req.recurrenceUntil());
        task.setCategoryId(resolveCategory(req.categoryId(), task.getWorkspaceId()));
        if (req.completed() != null) {
            task.setCompleted(req.completed());
        }
        // Stamp completion time for productivity stats (cleared if re-opened).
        if (!wasCompleted && task.isCompleted()) {
            task.setCompletedAt(Instant.now());
        } else if (wasCompleted && !task.isCompleted()) {
            task.setCompletedAt(null);
        }
        if (req.labelIds() != null) {
            task.setLabels(resolveLabels(req.labelIds(), task.getWorkspaceId()));
        }
        Task saved = taskRepository.save(task);

        // When a task is completed, notify its creator (unless they completed it themselves).
        if (!wasCompleted && saved.isCompleted()) {
            Long actor = SecurityUtils.getCurrentUserId();
            Long creator = saved.getUserId();
            if (creator != null && !creator.equals(actor)) {
                eventPublisher.publish(EventTypes.AGGREGATE_TASK, saved.getId(), EventTypes.TASK_COMPLETED,
                        new TaskEventPayload(EventTypes.TASK_COMPLETED, saved.getId(), saved.getTitle(),
                                saved.getWorkspaceId(), creator, actor, "Việc đã hoàn thành",
                                "Công việc \"" + saved.getTitle() + "\" đã được hoàn thành.", null));
            }
        }
        // When a recurring task is completed, spawn the next occurrence.
        if (!wasCompleted && saved.isCompleted() && saved.getRecurrenceFreq() != null) {
            spawnNextOccurrence(saved);
        }
        return dto(saved);
    }

    /**
     * Creates the next instance of a recurring task: a clone in the same column with due/remind shifted
     * forward by {@code freq × interval}, reset to not-completed. Skips if the next due passes the until date.
     */
    private void spawnNextOccurrence(Task task) {
        int interval = (task.getRecurrenceInterval() == null || task.getRecurrenceInterval() < 1)
                ? 1 : task.getRecurrenceInterval();
        Instant base = task.getDueDate() != null ? task.getDueDate() : Instant.now();
        Instant nextDue = shift(base, task.getRecurrenceFreq(), interval);
        if (task.getRecurrenceUntil() != null && nextDue.isAfter(task.getRecurrenceUntil())) {
            return; // recurrence finished
        }

        Task next = new Task();
        next.setUserId(task.getUserId());
        next.setWorkspaceId(task.getWorkspaceId());
        next.setBoardId(task.getBoardId());
        next.setColumnId(task.getColumnId());
        next.setCategoryId(task.getCategoryId());
        next.setAssigneeId(task.getAssigneeId());
        next.setTitle(task.getTitle());
        next.setDescription(task.getDescription());
        next.setPriority(task.getPriority());
        next.setDueDate(nextDue);
        next.setRemindAt(task.getRemindAt() != null
                ? shift(task.getRemindAt(), task.getRecurrenceFreq(), interval) : null);
        next.setReminderSent(false);
        next.setCompleted(false);
        next.setRecurrenceFreq(task.getRecurrenceFreq());
        next.setRecurrenceInterval(task.getRecurrenceInterval());
        next.setRecurrenceUntil(task.getRecurrenceUntil());
        next.setLabels(new HashSet<>(task.getLabels()));
        next.setPosition((int) taskRepository.countByColumnId(task.getColumnId()));
        taskRepository.save(next);
    }

    private Instant shift(Instant from, com.taskmanager.task.entity.RecurrenceFreq freq, int interval) {
        java.time.ZonedDateTime z = from.atZone(java.time.ZoneOffset.UTC);
        return switch (freq) {
            case DAILY -> z.plusDays(interval).toInstant();
            case WEEKLY -> z.plusWeeks(interval).toInstant();
            case MONTHLY -> z.plusMonths(interval).toInstant();
        };
    }

    @Transactional
    public void delete(Long id) {
        taskRepository.delete(loadOwned(id));
    }

    /**
     * Moves a task to {@code columnId} at {@code position}, renumbering siblings in both the source
     * and target columns so positions stay contiguous (0..n).
     */
    @Transactional
    public TaskDto move(Long id, MoveTaskRequest req) {
        Task task = loadOwned(id);
        BoardColumn target = columnRepository.findByIdAndWorkspaceId(req.columnId(), task.getWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Column", req.columnId()));
        if (!target.getBoardId().equals(task.getBoardId())) {
            throw new BadRequestException("Không thể chuyển task sang bảng khác");
        }
        Long oldColumnId = task.getColumnId();

        List<Task> targetTasks = taskRepository.findByColumnIdOrderByPositionAscIdAsc(req.columnId())
                .stream().filter(t -> !t.getId().equals(id)).collect(java.util.stream.Collectors.toList());
        int idx = Math.max(0, Math.min(req.position(), targetTasks.size()));
        task.setColumnId(req.columnId());
        targetTasks.add(idx, task);
        for (int i = 0; i < targetTasks.size(); i++) {
            targetTasks.get(i).setPosition(i);
        }
        taskRepository.saveAll(targetTasks);

        if (!oldColumnId.equals(req.columnId())) {
            List<Task> oldTasks = taskRepository.findByColumnIdOrderByPositionAscIdAsc(oldColumnId)
                    .stream().filter(t -> !t.getId().equals(id)).collect(java.util.stream.Collectors.toList());
            for (int i = 0; i < oldTasks.size(); i++) {
                oldTasks.get(i).setPosition(i);
            }
            taskRepository.saveAll(oldTasks);
        }
        return dto(task);
    }

    private Task loadOwned(Long id) {
        return taskRepository.findByIdAndWorkspaceId(id, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
    }

    private Long resolveCategory(Long categoryId, Long workspaceId) {
        if (categoryId == null) {
            return null;
        }
        categoryRepository.findByIdAndWorkspaceId(categoryId, workspaceId)
                .orElseThrow(() -> new BadRequestException("Danh mục không hợp lệ"));
        return categoryId;
    }

    private Set<Label> resolveLabels(List<Long> labelIds, Long workspaceId) {
        Set<Label> labels = new HashSet<>();
        if (labelIds == null || labelIds.isEmpty()) {
            return labels;
        }
        for (Long labelId : labelIds) {
            Label label = labelRepository.findByIdAndWorkspaceId(labelId, workspaceId)
                    .orElseThrow(() -> new BadRequestException("Nhãn không hợp lệ: " + labelId));
            labels.add(label);
        }
        return labels;
    }
}
