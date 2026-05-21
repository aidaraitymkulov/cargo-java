package com.cargoapp.backend.news.service;

import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.news.dto.CreateNewsRequest;
import com.cargoapp.backend.news.dto.NewsResponse;
import com.cargoapp.backend.news.dto.UpdateNewsRequest;
import com.cargoapp.backend.news.entity.NewsEntity;
import com.cargoapp.backend.news.mapper.NewsMapper;
import com.cargoapp.backend.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional(readOnly = true)
    public Page<NewsResponse> getAll(int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        return newsRepository.findAll(pageable)
                .map(newsMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public NewsResponse getById(UUID id) {
        return newsRepository.findById(id)
                .map(newsMapper::toResponse)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));
    }

    @Transactional
    public NewsResponse create(CreateNewsRequest request, MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Изображение обязательно");
        }

        var news = new NewsEntity();
        news.setTitle(request.title());
        news.setContent(request.content());
        news.setImage(saveImage(image));

        return newsMapper.toResponse(newsRepository.save(news));
    }

    @Transactional
    public NewsResponse update(UUID id, UpdateNewsRequest request, MultipartFile image) {
        var news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));

        if (request.title() != null) news.setTitle(request.title());
        if (request.content() != null) news.setContent(request.content());

        String oldImage = null;
        if (image != null && !image.isEmpty()) {
            oldImage = news.getImage();
            news.setImage(saveImage(image));
        }

        NewsResponse response = newsMapper.toResponse(newsRepository.save(news));
        deleteImage(oldImage);
        return response;
    }

    @Transactional
    public void delete(UUID id) {
        var news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));
        deleteImage(news.getImage());
        newsRepository.delete(news);
    }

    private void deleteImage(String coverUrl) {
        if (coverUrl == null) return;
        try {
            String filename = coverUrl.replace("/uploads/news/", "");
            Path path = Paths.get(uploadDir, "news", filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete image file '{}': {}", coverUrl, e.getMessage(), e);
        }
    }

    private String saveImage(MultipartFile file) {
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
            Path path = Paths.get(uploadDir, "news", filename);
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            return "/uploads/news/" + filename;
        } catch (IOException e) {
            throw new AppException("IMAGE_UPLOAD_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при сохранении изображения");
        }
    }
}
