package com.cargoapp.backend.common.storage;

import com.cargoapp.backend.common.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class ImageStorageService {

    @Value("${app.upload-dir}")
    private String uploadDir;

    public String save(MultipartFile file, String subDir) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Разрешены только изображения");
        }
        try {
            String original = file.getOriginalFilename();
            String safeName = (original != null)
                    ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "upload";
            String filename = UUID.randomUUID() + "_" + safeName;
            Path path = Paths.get(uploadDir, subDir, filename);
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            return "/uploads/" + subDir + "/" + filename;
        } catch (IOException e) {
            log.error("Failed to save image to '{}/{}': {}", uploadDir, subDir, e.getMessage(), e);
            throw new AppException("IMAGE_UPLOAD_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при сохранении изображения");
        }
    }

    public void delete(String url, String subDir) {
        if (url == null) return;
        try {
            String filename = url.replace("/uploads/" + subDir + "/", "");
            Path path = Paths.get(uploadDir, subDir, filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete image '{}': {}", url, e.toString(), e);
        }
    }
}