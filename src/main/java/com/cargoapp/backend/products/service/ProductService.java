package com.cargoapp.backend.products.service;

import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.products.dto.*;
import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductStatus;
import com.cargoapp.backend.products.mapper.ProductMapper;
import com.cargoapp.backend.products.repository.ProductHistoryRepository;
import com.cargoapp.backend.products.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;
    private final ProductMapper productMapper;

    public PagedResponse<ProductResponse> getMyProducts(UUID userId, String status, int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<ProductEntity> result = status != null
                ? productRepository.findByUser_IdAndStatus(userId, parseProductStatus(status), pageable)
                : productRepository.findByUser_Id(userId, pageable);

        var items = result.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }

    public ProductResponse getProductById(UUID userId, UUID productId) {
        ProductEntity product = productRepository.findByIdAndUser_Id(productId, userId)
                .orElseThrow(() -> new AppException("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "Товар не найден"));
        return productMapper.toProductResponse(product);
    }

    public ProductHistoryResponse getProductHistory(UUID userId, UUID productId) {
        productRepository.findByIdAndUser_Id(productId, userId)
                .orElseThrow(() -> new AppException("PRODUCT_NOT_FOUND", HttpStatus.NOT_FOUND, "Товар не найден"));

        var items = productHistoryRepository.findByProduct_IdOrderByCreatedAtAsc(productId)
                .stream()
                .map(productMapper::toHistoryEntry)
                .toList();

        return new ProductHistoryResponse(items, items.size());
    }

    public ItemsSummaryResponse getItemsSummary(UUID userId) {
        // Инициализируем все статусы нулями
        Map<String, Long> counts = new LinkedHashMap<>();
        for (ProductStatus s : ProductStatus.values()) {
            counts.put(s.name(), 0L);
        }
        // Заполняем реальными данными
        productRepository.countByStatusForUser(userId)
                .forEach(row -> counts.put(((ProductStatus) row[0]).name(), (Long) row[1]));

        LocalDateTime lastUpdatedAt = productRepository.findLastUpdatedAtByUserId(userId).orElse(null);

        // TODO: заполнить когда будет orders домен
        return new ItemsSummaryResponse(counts, 0L, 0L, lastUpdatedAt);
    }

    public List<ProductResponse> getProductsByOrderId(UUID orderId) {
        return productRepository.findByOrderId(orderId).stream()
                .map(productMapper::toProductResponse)
                .toList();
    }

    private ProductStatus parseProductStatus(String status) {
        try {
            return ProductStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Неверный статус: " + status);
        }
    }

    public PagedResponse<ProductAdminResponse> getUserProductsForAdmin(UUID userId, int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        Page<ProductEntity> result = productRepository.findByUser_Id(userId, pageable);

        var items = result.getContent().stream()
                .map(productMapper::toProductAdminResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }
}
