package com.cargoapp.backend.news.repository;

import com.cargoapp.backend.news.entity.NewsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface NewsRepository extends JpaRepository<NewsEntity, UUID> {
}
