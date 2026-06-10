package com.taskmanager.attachment.service;

import com.taskmanager.common.exception.BadRequestException;
import com.taskmanager.common.exception.ResourceNotFoundException;
import com.taskmanager.config.AppProperties;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Stores uploaded files on a local volume (app.storage.location, default ./uploads).
 * Each file is saved under an opaque generated name; original name lives in the DB.
 */
@Service
public class FileStorageService {

    private final Path root;

    public FileStorageService(AppProperties properties) {
        this.root = Paths.get(properties.getStorage().getLocation()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException("Không khởi tạo được thư mục lưu trữ: " + root, e);
        }
    }

    public void store(MultipartFile file, String storedName) {
        Path target = root.resolve(storedName).normalize();
        if (!target.startsWith(root)) {
            throw new BadRequestException("Tên tệp không hợp lệ");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new IllegalStateException("Lưu tệp thất bại", e);
        }
    }

    public Resource loadAsResource(String storedName) {
        try {
            Path file = root.resolve(storedName).normalize();
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            }
            throw new ResourceNotFoundException("File", storedName);
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", storedName);
        }
    }

    public void delete(String storedName) {
        try {
            Files.deleteIfExists(root.resolve(storedName).normalize());
        } catch (IOException ignored) {
            // best-effort cleanup; DB row is the source of truth
        }
    }
}
