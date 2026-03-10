package com.cargoapp.backend.imports.service;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.imports.parser.ExcelImportParser;
import com.cargoapp.backend.orders.entity.OrderEntity;
import com.cargoapp.backend.orders.entity.OrderStatus;
import com.cargoapp.backend.orders.repository.OrderRepository;
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

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Обработчик отдельных блоков KG-импорта.
 * <p>
 * Вынесен в отдельный @Service намеренно — это решение проблемы self-invocation в Spring.
 * REQUIRES_NEW = "открой новую транзакцию, даже если уже есть активная".
 * Это позволяет обрабатывать каждый блок независимо — ошибка в одном не откатывает другие.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgBlockProcessor {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;

    // =========================================================================
    // in-kg блок
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processInKgBlock(ExcelImportParser.KgBlock block, BranchEntity branch) {
        UserEntity user = findUserByPersonalCode(block.personalCode());

        // Создаём заказ
        OrderEntity order = new OrderEntity();
        order.setUser(user);
        order.setBranch(branch);
        order.setWeight(block.weight());
        order.setPrice(block.price());
        order.setItemCount(block.hatches().size());
        order.setStatus(OrderStatus.PENDING_PICKUP);
        orderRepository.save(order);

        // Обрабатываем каждый hatch блока
        for (String hatch : block.hatches()) {
            attachOrCreateProduct(hatch, user, order);
        }

        log.info("in-kg block processed: personalCode={}, order={}, items={}",
                block.personalCode(), order.getId(), block.hatches().size());
    }

    // =========================================================================
    // delivered блок
    // =========================================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processDeliveredBlock(ExcelImportParser.KgBlock block, BranchEntity branch) {
        UserEntity user = findUserByPersonalCode(block.personalCode());

        OrderEntity order = findOrderByUserAndHatches(user, branch, block.hatches(), block.summaryRowNumber());

        if (order.getStatus() == OrderStatus.DELIVERED) {
            throw new AppException(
                    "ORDER_ALREADY_DELIVERED",
                    HttpStatus.CONFLICT,
                    "Заказ уже выдан (orderId=" + order.getId() + ")"
            );
        }

        // Переводим заказ и все товары в DELIVERED
        order.setStatus(OrderStatus.DELIVERED);
        orderRepository.save(order);

        List<ProductEntity> products = productRepository.findByOrderId(order.getId());
        for (ProductEntity product : products) {
            if (product.getStatus() != ProductStatus.DELIVERED) {
                product.setStatus(ProductStatus.DELIVERED);
                productRepository.save(product);
                logProductHistory(product, ProductStatus.DELIVERED);
            }
        }

        log.info("delivered block processed: personalCode={}, order={}, products={}",
                block.personalCode(), order.getId(), products.size());
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
     * FIFO-поиск: ищем самый старый непривязанный товар с нужным hatch у этого клиента.
     * Если не нашли — создаём новый сразу с IN_KG.
     */
    private void attachOrCreateProduct(String hatch, UserEntity user, OrderEntity order) {
        Optional<ProductEntity> existing = productRepository
                .findFirstByUser_IdAndHatchAndOrderIdIsNullAndStatusNotOrderByCreatedAtAsc(
                        user.getId(),
                        hatch,
                        ProductStatus.DELIVERED
                );

        ProductEntity product;
        if (existing.isPresent()) {
            product = existing.get();
            product.setOrderId(order.getId());
            product.setStatus(ProductStatus.IN_KG);
            productRepository.save(product);
            logProductHistory(product, ProductStatus.IN_KG);
        } else {
            product = new ProductEntity();
            product.setHatch(hatch);
            product.setUser(user);
            product.setOrderId(order.getId());
            product.setStatus(ProductStatus.IN_KG);
            productRepository.save(product);
            logProductHistory(product, ProductStatus.IN_KG);
        }
    }

    /**
     * Поиск заказа по клиенту + набору hatch.
     * Сравниваем Set<String> hatch заказа с тем что пришло в файле.
     */
    private OrderEntity findOrderByUserAndHatches(UserEntity user,
                                                  BranchEntity branch,
                                                  List<String> expectedHatches,
                                                  int rowNum) {
        List<OrderEntity> pendingOrders = orderRepository
                .findByUser_IdAndBranch_IdAndStatus(user.getId(), branch.getId(), OrderStatus.PENDING_PICKUP);

        Set<String> expectedSet = new HashSet<>(expectedHatches);

        for (OrderEntity order : pendingOrders) {
            List<ProductEntity> products = productRepository.findByOrderId(order.getId());
            Set<String> orderHatches = new HashSet<>();
            for (ProductEntity p : products) {
                orderHatches.add(p.getHatch());
            }
            if (orderHatches.equals(expectedSet)) {
                return order;
            }
        }

        throw new AppException(
                "ORDER_NOT_FOUND",
                HttpStatus.NOT_FOUND,
                "Заказ для personalCode=" + user.getPersonalCode() +
                        " с указанными hatch не найден (строка " + rowNum + ")"
        );
    }

    private void logProductHistory(ProductEntity product, ProductStatus status) {
        ProductHistoryEntity history = new ProductHistoryEntity();
        history.setProduct(product);
        history.setStatus(status);
        productHistoryRepository.save(history);
    }
}
