package com.cargoapp.backend.news.controller.admin;

import com.cargoapp.backend.news.dto.CreateNewsRequest;
import com.cargoapp.backend.news.dto.NewsResponse;
import com.cargoapp.backend.news.dto.UpdateNewsRequest;
import com.cargoapp.backend.news.service.NewsService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/admin/news")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class NewsAdminController {

    private final NewsService newsService;

    @GetMapping
    public Page<NewsResponse> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return newsService.getAll(page, pageSize);
    }

    @GetMapping("/{newsId}")
    public NewsResponse getById(@PathVariable UUID newsId) {
        return newsService.getById(newsId);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public NewsResponse create(
            @RequestPart("data") @Valid CreateNewsRequest request,
            @RequestPart("image") MultipartFile image
    ) {
        return newsService.create(request, image);
    }

    @PatchMapping(value = "/{newsId}", consumes = "multipart/form-data")
    public NewsResponse update(
            @PathVariable UUID newsId,
            @RequestPart("data") @Valid UpdateNewsRequest request,
            @RequestPart(value = "image", required = false) MultipartFile image
    ) {
        return newsService.update(newsId, request, image);
    }

    @DeleteMapping("/{newsId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID newsId) {
        newsService.delete(newsId);
    }
}
