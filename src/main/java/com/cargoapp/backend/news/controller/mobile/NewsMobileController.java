package com.cargoapp.backend.news.controller.mobile;

import com.cargoapp.backend.news.dto.NewsResponse;
import com.cargoapp.backend.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/news")
@RequiredArgsConstructor
public class NewsMobileController {

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
}
