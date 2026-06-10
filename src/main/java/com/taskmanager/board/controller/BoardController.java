package com.taskmanager.board.controller;

import com.taskmanager.board.dto.BoardDetailDto;
import com.taskmanager.board.dto.BoardDto;
import com.taskmanager.board.dto.BoardRequest;
import com.taskmanager.board.dto.ColumnDto;
import com.taskmanager.board.dto.ColumnRequest;
import com.taskmanager.board.dto.ReorderColumnsRequest;
import com.taskmanager.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/boards")
@Tag(name = "Boards")
public class BoardController {

    private final BoardService service;

    public BoardController(BoardService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List boards (creates a default board on first use)")
    public List<BoardDto> list() {
        return service.listBoards();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full Kanban board (columns + tasks)")
    public BoardDetailDto detail(@PathVariable Long id) {
        return service.getDetail(id);
    }

    @PostMapping
    @Operation(summary = "Create a board with default columns")
    public ResponseEntity<BoardDto> create(@Valid @RequestBody BoardRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createBoard(request));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a board")
    public BoardDto update(@PathVariable Long id, @Valid @RequestBody BoardRequest request) {
        return service.updateBoard(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a board")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteBoard(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/columns")
    @Operation(summary = "Add a column to a board")
    public ResponseEntity<ColumnDto> addColumn(@PathVariable Long id, @Valid @RequestBody ColumnRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.addColumn(id, request));
    }

    @PatchMapping("/{id}/columns/reorder")
    @Operation(summary = "Reorder a board's columns")
    public ResponseEntity<Void> reorderColumns(@PathVariable Long id, @Valid @RequestBody ReorderColumnsRequest request) {
        service.reorderColumns(id, request);
        return ResponseEntity.noContent().build();
    }
}
