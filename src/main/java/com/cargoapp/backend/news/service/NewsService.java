package com.cargoapp.backend.news.service;

import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.common.storage.ImageStorageService;
import com.cargoapp.backend.news.dto.CreateNewsRequest;
import com.cargoapp.backend.news.dto.NewsResponse;
import com.cargoapp.backend.news.dto.UpdateNewsRequest;
import com.cargoapp.backend.news.entity.NewsEntity;
import com.cargoapp.backend.news.mapper.NewsMapper;
import com.cargoapp.backend.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NewsService {

    private static final String SUBDIR = "news";

    private final NewsRepository newsRepository;
    private final NewsMapper newsMapper;
    private final ImageStorageService imageStorageService;

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
        news.setImage(imageStorageService.save(image, SUBDIR));

        return newsMapper.toResponse(newsRepository.save(news));
    }

    @Transactional
    public NewsResponse update(UUID id, UpdateNewsRequest request, MultipartFile image) {
        var news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));

        if (request.title() != null) news.setTitle(request.title());
        if (request.content() != null) news.setContent(request.content());

        if (image != null && !image.isEmpty()) {
            String oldImage = news.getImage();
            news.setImage(imageStorageService.save(image, SUBDIR));
            imageStorageService.delete(oldImage, SUBDIR);
        }

        return newsMapper.toResponse(newsRepository.save(news));
    }

    @Transactional
    public void delete(UUID id) {
        var news = newsRepository.findById(id)
                .orElseThrow(() -> new AppException("NEWS_NOT_FOUND", HttpStatus.NOT_FOUND, "Новость не найдена"));
        imageStorageService.delete(news.getImage(), SUBDIR);
        newsRepository.delete(news);
    }
}