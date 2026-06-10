package com.taskmanager.board.controller;

import com.taskmanager.board.dto.ColumnDto;
import com.taskmanager.board.dto.ColumnRequest;
import com.taskmanager.board.service.BoardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/columns")
@Tag(name = "Boards")
public class ColumnController {

    private final BoardService service;

    public ColumnController(BoardService service) {
        this.service = service;
    }

    @PutMapping("/{id}")
    @Operation(summary = "Rename a column")
    public ColumnDto update(@PathVariable Long id, @Valid @RequestBody ColumnRequest request) {
        return service.updateColumn(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a column (and its tasks)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteColumn(id);
        return ResponseEntity.noContent().build();
    }
}
