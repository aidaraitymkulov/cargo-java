package com.cargoapp.backend.products.service;

import com.cargoapp.backend.products.entity.ProductStatus;
import com.cargoapp.backend.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductScheduler {

    private final ProductRepository productRepository;

    @Scheduled(fixedRate = 12 * 60 * 60 * 1000)
    @Transactional
    public void promoteInChinaToOnTheWay() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(12);
        int updated = productRepository.bulkUpdateStatus(
                ProductStatus.IN_CHINA, ProductStatus.ON_THE_WAY, threshold);
        if (updated > 0) {
            log.info("Cron: promoted {} products IN_CHINA → ON_THE_WAY", updated);
        }
    }
}