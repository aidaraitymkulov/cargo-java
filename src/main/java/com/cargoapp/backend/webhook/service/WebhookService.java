package com.cargoapp.backend.webhook.service;

import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductHistoryEntity;
import com.cargoapp.backend.products.entity.ProductStatus;
import com.cargoapp.backend.products.repository.ProductHistoryRepository;
import com.cargoapp.backend.products.repository.ProductRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WebhookService {

    private static final Map<String, ProductStatus> STATUS_MAP = Map.of(
            "AWAIT_POSTING", ProductStatus.IN_CHINA,
            "ON_WAY",        ProductStatus.ON_THE_WAY,
            "ON_PVZ",        ProductStatus.IN_KG,
            "ISSUED",        ProductStatus.DELIVERED
    );

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final ProductHistoryRepository productHistoryRepository;

    @Transactional
    public void handleStatus(String trackNumber, String spStatus, String clientCode,
                             BigDecimal weight, BigDecimal price) {
        ProductStatus ourStatus = STATUS_MAP.get(spStatus);
        if (ourStatus == null) {
            log.info("Webhook: unknown SmartPoint status={}, skipping track={}", spStatus, trackNumber);
            return;
        }

        Optional<ProductEntity> existing = productRepository
                .findFirstByUser_PersonalCodeAndHatchAndStatusNotOrderByCreatedAtAsc(
                        clientCode, trackNumber, ProductStatus.DELIVERED);

        if (existing.isEmpty()) {
            createProduct(trackNumber, clientCode, ourStatus, weight, price);
        } else {
            updateProduct(existing.get(), ourStatus, weight, price, trackNumber);
        }
    }

    private void createProduct(String trackNumber, String clientCode, ProductStatus status,
                               BigDecimal weight, BigDecimal price) {
        UserEntity user = userRepository.findByPersonalCode(clientCode).orElse(null);
        if (user == null) {
            log.warn("Webhook: personalCode={} not found, skipping track={}", clientCode, trackNumber);
            return;
        }

        ProductEntity product = new ProductEntity();
        product.setHatch(trackNumber);
        product.setUser(user);
        product.setStatus(status);
        product.setWeight(weight);
        product.setPrice(price);
        productRepository.save(product);
        saveHistory(product, status);

        log.info("Webhook: created track={} personalCode={} status={}", trackNumber, clientCode, status);
    }

    private void updateProduct(ProductEntity product, ProductStatus newStatus,
                               BigDecimal weight, BigDecimal price, String trackNumber) {
        if (product.getStatus() == newStatus) {
            log.info("Webhook: track={} already in status={}, skipping", trackNumber, newStatus);
            return;
        }

        product.setStatus(newStatus);
        if (weight != null) product.setWeight(weight);
        if (price != null) product.setPrice(price);
        productRepository.save(product);
        saveHistory(product, newStatus);

        log.info("Webhook: updated track={} → status={}", trackNumber, newStatus);
    }

    private void saveHistory(ProductEntity product, ProductStatus status) {
        ProductHistoryEntity history = new ProductHistoryEntity();
        history.setProduct(product);
        history.setStatus(status);
        productHistoryRepository.save(history);
    }
}