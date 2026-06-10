package com.taskmanager.attachment.controller;

import com.taskmanager.attachment.dto.AttachmentDto;
import com.taskmanager.attachment.service.AttachmentService;
import com.taskmanager.attachment.service.AttachmentService.DownloadResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@Tag(name = "Attachments")
public class AttachmentController {

    private final AttachmentService service;

    public AttachmentController(AttachmentService service) {
        this.service = service;
    }

    @GetMapping("/v1/tasks/{taskId}/attachments")
    @Operation(summary = "List attachments of a task")
    public List<AttachmentDto> list(@PathVariable Long taskId) {
        return service.list(taskId);
    }

    @PostMapping(value = "/v1/tasks/{taskId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload an attachment to a task")
    public ResponseEntity<AttachmentDto> upload(@PathVariable Long taskId,
                                                @RequestParam("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.upload(taskId, file));
    }

    @GetMapping("/v1/attachments/{id}/download")
    @Operation(summary = "Download an attachment")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        DownloadResource d = service.loadForDownload(id);
        String encoded = URLEncoder.encode(d.filename(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(d.contentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename*=UTF-8''" + encoded)
                .body(d.resource());
    }

    @DeleteMapping("/v1/attachments/{id}")
    @Operation(summary = "Delete an attachment")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
