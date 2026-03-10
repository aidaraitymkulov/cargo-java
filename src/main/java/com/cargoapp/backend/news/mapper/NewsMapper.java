package com.cargoapp.backend.news.mapper;

import com.cargoapp.backend.news.dto.NewsResponse;
import com.cargoapp.backend.news.entity.NewsEntity;
import org.springframework.stereotype.Component;

@Component
public class NewsMapper {
    public NewsResponse toResponse(NewsEntity news) {
        return new NewsResponse(
                news.getId(),
                news.getCover(),
                news.getTitle(),
                news.getContent(),
                news.getCreatedAt(),
                news.getUpdatedAt()
        );
    }
}
