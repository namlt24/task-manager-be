package com.taskmanager.board.service;

import com.taskmanager.board.dto.BoardDetailDto;
import com.taskmanager.board.dto.BoardDto;
import com.taskmanager.board.dto.BoardRequest;
import com.taskmanager.board.dto.ColumnDto;
import com.taskmanager.board.dto.ColumnRequest;
import com.taskmanager.board.dto.ReorderColumnsRequest;
import com.taskmanager.board.entity.Board;
import com.taskmanager.board.entity.BoardColumn;
import com.taskmanager.board.repository.BoardColumnRepository;
import com.taskmanager.board.repository.BoardRepository;
import com.taskmanager.attachment.entity.Attachment;
import com.taskmanager.attachment.repository.AttachmentRepository;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.security.SecurityUtils;
import com.taskmanager.subtask.entity.Subtask;
import com.taskmanager.subtask.repository.SubtaskRepository;
import com.taskmanager.task.entity.Task;
import com.taskmanager.task.mapper.TaskMapper;
import com.taskmanager.task.repository.TaskRepository;
import com.taskmanager.timetracking.repository.TimeEntryRepository;
import com.taskmanager.workspace.WorkspaceAccess;
import com.taskmanager.workspace.entity.WorkspaceRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BoardService {

    private static final List<String> DEFAULT_COLUMNS = List.of("Cần làm", "Đang làm", "Hoàn thành");

    private final BoardRepository boardRepository;
    private final BoardColumnRepository columnRepository;
    private final TaskRepository taskRepository;
    private final SubtaskRepository subtaskRepository;
    private final AttachmentRepository attachmentRepository;
    private final TimeEntryRepository timeEntryRepository;
    private final TaskMapper taskMapper;
    private final WorkspaceAccess access;

    public BoardService(BoardRepository boardRepository,
                        BoardColumnRepository columnRepository,
                        TaskRepository taskRepository,
                        SubtaskRepository subtaskRepository,
                        AttachmentRepository attachmentRepository,
                        TimeEntryRepository timeEntryRepository,
                        TaskMapper taskMapper,
                        WorkspaceAccess access) {
        this.boardRepository = boardRepository;
        this.columnRepository = columnRepository;
        this.taskRepository = taskRepository;
        this.subtaskRepository = subtaskRepository;
        this.attachmentRepository = attachmentRepository;
        this.timeEntryRepository = timeEntryRepository;
        this.taskMapper = taskMapper;
        this.access = access;
    }

    /** Lists the workspace's boards, provisioning a default board on first use (managers only). */
    @Transactional
    public List<BoardDto> listBoards() {
        Long workspaceId = access.currentWorkspaceId();
        List<Board> boards = boardRepository.findByWorkspaceIdOrderByPositionAscIdAsc(workspaceId);
        if (boards.isEmpty() && access.currentRole() != WorkspaceRole.MEMBER) {
            boards = List.of(createBoardWithDefaults(workspaceId, SecurityUtils.getCurrentUserId(), "Bảng của tôi"));
        }
        return boards.stream().map(this::toDto).toList();
    }

    @Transactional
    public BoardDto createBoard(BoardRequest request) {
        Long workspaceId = access.requireManager();
        return toDto(createBoardWithDefaults(workspaceId, SecurityUtils.getCurrentUserId(), request.name()));
    }

    @Transactional
    public BoardDto updateBoard(Long id, BoardRequest request) {
        access.requireManager();
        Board board = loadBoard(id);
        board.setName(request.name());
        return toDto(boardRepository.save(board));
    }

    @Transactional
    public void deleteBoard(Long id) {
        access.requireManager();
        boardRepository.delete(loadBoard(id));
    }

    /** Full Kanban payload: board + ordered columns, each with its ordered tasks. */
    @Transactional(readOnly = true)
    public BoardDetailDto getDetail(Long boardId) {
        Board board = loadBoard(boardId);
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderByPositionAscIdAsc(board.getId());
        List<Task> tasks = taskRepository.findByBoardIdOrderByColumnIdAscPositionAscIdAsc(board.getId());
        Map<Long, List<Task>> tasksByColumn = tasks.stream()
                .collect(Collectors.groupingBy(Task::getColumnId));

        // Batch-load subtask + attachment counts + tracked time for all tasks of the board (avoids N+1).
        Map<Long, Integer> total = new java.util.HashMap<>();
        Map<Long, Integer> done = new java.util.HashMap<>();
        Map<Long, Integer> attach = new java.util.HashMap<>();
        Map<Long, Long> tracked = new java.util.HashMap<>();
        List<Long> taskIds = tasks.stream().map(Task::getId).toList();
        if (!taskIds.isEmpty()) {
            for (Subtask s : subtaskRepository.findByTaskIdInOrderByPositionAscIdAsc(taskIds)) {
                total.merge(s.getTaskId(), 1, Integer::sum);
                if (s.isCompleted()) {
                    done.merge(s.getTaskId(), 1, Integer::sum);
                }
            }
            for (Attachment a : attachmentRepository.findByTaskIdIn(taskIds)) {
                attach.merge(a.getTaskId(), 1, Integer::sum);
            }
            for (Object[] row : timeEntryRepository.sumDurationByTaskIds(taskIds)) {
                tracked.put((Long) row[0], ((Number) row[1]).longValue());
            }
        }

        List<ColumnDto> columnDtos = columns.stream().map(col -> new ColumnDto(
                col.getId(),
                col.getName(),
                col.getPosition(),
                tasksByColumn.getOrDefault(col.getId(), List.of()).stream()
                        .map(t -> taskMapper.toDto(t,
                                total.getOrDefault(t.getId(), 0),
                                done.getOrDefault(t.getId(), 0),
                                attach.getOrDefault(t.getId(), 0),
                                tracked.getOrDefault(t.getId(), 0L)))
                        .toList()
        )).toList();

        return new BoardDetailDto(board.getId(), board.getName(), columnDtos);
    }

    @Transactional
    public ColumnDto addColumn(Long boardId, ColumnRequest request) {
        access.requireManager();
        Board board = loadBoard(boardId);
        BoardColumn column = new BoardColumn();
        column.setBoardId(board.getId());
        column.setUserId(SecurityUtils.getCurrentUserId());
        column.setWorkspaceId(board.getWorkspaceId());
        column.setName(request.name());
        column.setPosition((int) columnRepository.countByBoardId(board.getId()));
        column = columnRepository.save(column);
        return new ColumnDto(column.getId(), column.getName(), column.getPosition(), List.of());
    }

    @Transactional
    public ColumnDto updateColumn(Long columnId, ColumnRequest request) {
        access.requireManager();
        BoardColumn column = loadColumn(columnId);
        column.setName(request.name());
        column = columnRepository.save(column);
        return new ColumnDto(column.getId(), column.getName(), column.getPosition(), List.of());
    }

    @Transactional
    public void deleteColumn(Long columnId) {
        access.requireManager();
        columnRepository.delete(loadColumn(columnId));
    }

    @Transactional
    public void reorderColumns(Long boardId, ReorderColumnsRequest request) {
        access.requireManager();
        Board board = loadBoard(boardId);
        List<BoardColumn> columns = columnRepository.findByBoardIdOrderByPositionAscIdAsc(board.getId());
        Map<Long, BoardColumn> byId = columns.stream()
                .collect(Collectors.toMap(BoardColumn::getId, c -> c));
        int pos = 0;
        for (Long colId : request.columnIds()) {
            BoardColumn col = byId.get(colId);
            if (col != null) {
                col.setPosition(pos++);
            }
        }
        columnRepository.saveAll(columns);
    }

    // ---- helpers ----

    private Board createBoardWithDefaults(Long workspaceId, Long userId, String name) {
        Board board = new Board();
        board.setUserId(userId);
        board.setWorkspaceId(workspaceId);
        board.setName(name);
        board.setPosition((int) boardRepository.countByWorkspaceId(workspaceId));
        board = boardRepository.save(board);
        int pos = 0;
        for (String colName : DEFAULT_COLUMNS) {
            BoardColumn column = new BoardColumn();
            column.setBoardId(board.getId());
            column.setUserId(userId);
            column.setWorkspaceId(workspaceId);
            column.setName(colName);
            column.setPosition(pos++);
            columnRepository.save(column);
        }
        return board;
    }

    private Board loadBoard(Long id) {
        return boardRepository.findByIdAndWorkspaceId(id, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Board", id));
    }

    private BoardColumn loadColumn(Long id) {
        return columnRepository.findByIdAndWorkspaceId(id, access.currentWorkspaceId())
                .orElseThrow(() -> new ResourceNotFoundException("Column", id));
    }

    private BoardDto toDto(Board board) {
        return new BoardDto(board.getId(), board.getName(), board.getPosition());
    }
}
