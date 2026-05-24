package com.cargoapp.backend.imports.service;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.imports.parser.ExcelImportParser;
import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductHistoryEntity;
import com.cargoapp.backend.products.entity.ProductStatus;
import com.cargoapp.backend.products.repository.ProductHistoryRepository;
import com.cargoapp.backend.products.repository.ProductRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * Обработчик отдельных блоков KG-импорта.
 *
 * Вынесен в отдельный @Service намеренно — это решение проблемы self-invocation в Spring.
 * REQUIRES_NEW = "открой новую транзакцию, даже если уже есть активная".
 * Это позволяет обрабатывать каждый блок независимо — ошибка в одном не откатывает другие.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgBlockProcessor {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;

    // =========================================================================
    // in-kg блок
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processInKgBlock(ExcelImportParser.KgBlock block, BranchEntity branch) {
        UserEntity user = findUserByPersonalCode(block.personalCode());

        for (String hatch : block.hatches()) {
            attachOrCreateProduct(hatch, user, block.price(), block.weight());
        }

        log.info("in-kg block processed: personalCode={}, items={}",
                block.personalCode(), block.hatches().size());
    }

    // =========================================================================
    // delivered блок
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDeliveredBlock(ExcelImportParser.KgBlock block, BranchEntity branch) {
        UserEntity user = findUserByPersonalCode(block.personalCode());

        for (String hatch : block.hatches()) {
            ProductEntity product = productRepository
                    .findFirstByUser_IdAndHatchAndStatusOrderByCreatedAtAsc(
                            user.getId(), hatch, ProductStatus.IN_KG)
                    .orElseThrow(() -> new AppException(
                            "PRODUCT_NOT_FOUND",
                            HttpStatus.NOT_FOUND,
                            "Товар не найден: personalCode=" + block.personalCode() + ", hatch=" + hatch
                    ));

            product.setStatus(ProductStatus.DELIVERED);
            productRepository.save(product);
            logProductHistory(product, ProductStatus.DELIVERED);
        }

        log.info("delivered block processed: personalCode={}, items={}",
                block.personalCode(), block.hatches().size());
    }

    // =========================================================================
    // Вспомогательные методы
    // =========================================================================

    private UserEntity findUserByPersonalCode(String personalCode) {
        return userRepository.findByPersonalCode(personalCode)
                .orElseThrow(() -> new AppException(
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "personalCode " + personalCode + " не найден"
                ));
    }

    /**
     * FIFO-поиск: берём самый старый товар с данным hatch у клиента, не доставленный.
     * Если не нашли — создаём новый сразу со статусом IN_KG.
     */
    private void attachOrCreateProduct(String hatch, UserEntity user, BigDecimal price, BigDecimal weight) {
        Optional<ProductEntity> existing = productRepository
                .findFirstByUser_IdAndHatchAndStatusNotOrderByCreatedAtAsc(
                        user.getId(), hatch, ProductStatus.DELIVERED);

        ProductEntity product;
        if (existing.isPresent()) {
            product = existing.get();
        } else {
            product = new ProductEntity();
            product.setHatch(hatch);
            product.setUser(user);
        }

        product.setPrice(price);
        product.setWeight(weight);
        product.setStatus(ProductStatus.IN_KG);
        productRepository.save(product);
        logProductHistory(product, ProductStatus.IN_KG);
    }

    private void logProductHistory(ProductEntity product, ProductStatus status) {
        ProductHistoryEntity history = new ProductHistoryEntity();
        history.setProduct(product);
        history.setStatus(status);
        productHistoryRepository.save(history);
    }
}
