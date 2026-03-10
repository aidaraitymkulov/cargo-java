package com.cargoapp.backend.imports.service;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.imports.dto.ImportErrorDto;
import com.cargoapp.backend.imports.dto.ImportResultResponse;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервис импорта товаров из Китая.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChinaImportService {

    private final ExcelImportParser parser;
    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final ProductRepository productRepository;
    private final ProductHistoryRepository productHistoryRepository;

    /**
     * Импорт посылок из Китая.
     *
     * @param file          .xlsx файл
     * @param currentUserId UUID текущего пользователя (для проверки прав MANAGER)
     * @param isManager     true если роль MANAGER (иначе SUPER_ADMIN — без ограничений по филиалу)
     */
    @Transactional
    public ImportResultResponse importFromChina(MultipartFile file,
                                                UUID currentUserId,
                                                boolean isManager) {
        List<ExcelImportParser.ChinaRow> rows;
        try {
            rows = parser.parseChinaFile(file);
        } catch (IOException e) {
            log.error("Failed to parse China import file", e);
            throw new AppException("IMPORT_ERROR", HttpStatus.BAD_REQUEST, "Не удалось прочитать файл Excel");
        }

        // Если MANAGER — проверяем что он не импортирует чужих клиентов.
        // Для SUPER_ADMIN эту проверку пропускаем.
        UserEntity currentUser = null;
        if (isManager) {
            currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));
        }

        int imported = 0;
        List<ImportErrorDto> errors = new ArrayList<>();

        for (ExcelImportParser.ChinaRow row : rows) {
            try {
                processChinaRow(row, currentUser, isManager);
                imported++;
            } catch (AppException ex) {
                errors.add(new ImportErrorDto(row.rowNumber(), ex.getErrorCode(), ex.getMessage()));
                log.warn("China import row {} skipped: {} — {}", row.rowNumber(), ex.getErrorCode(), ex.getMessage());
            } catch (Exception ex) {
                errors.add(new ImportErrorDto(row.rowNumber(), "IMPORT_ERROR", ex.getMessage()));
                log.error("China import row {} unexpected error", row.rowNumber(), ex);
            }
        }

        log.info("China import finished: imported={}, errors={}", imported, errors.size());
        return new ImportResultResponse(imported, errors);
    }

    /**
     * Обрабатывает одну строку из файла.
     * Намеренно НЕ помечена @Transactional — она выполняется в контексте родительской транзакции.
     * При ошибке бросаем AppException, а caller её ловит и добавляет в errors[].
     * <p>
     * Почему не REQUIRES_NEW? Потому что для CN-импорта нам не нужны частичные откаты —
     * мы ловим исключение ДО вызова save, так что транзакция остаётся чистой.
     */
    private void processChinaRow(ExcelImportParser.ChinaRow row,
                                 UserEntity currentUser,
                                 boolean isManager) {
        UserEntity targetUser = userRepository.findByPersonalCode(row.personalCode())
                .orElseThrow(() -> new AppException(
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "personalCode " + row.personalCode() + " не найден"
                ));

        // Проверка прав MANAGER: он может импортировать только клиентов своего филиала
        if (isManager && currentUser != null) {
            validateManagerBranchAccess(currentUser, targetUser.getBranch());
        }

        // Создаём Product
        ProductEntity product = new ProductEntity();
        product.setHatch(row.hatch());
        product.setUser(targetUser);
        product.setStatus(ProductStatus.IN_CHINA);
        productRepository.save(product);

        // Логируем создание в истории
        logProductHistory(product, ProductStatus.IN_CHINA);
    }

    private void validateManagerBranchAccess(UserEntity manager, BranchEntity targetBranch) {
        if (manager.getBranch() == null) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Менеджер не привязан к филиалу");
        }
        if (targetBranch == null || !manager.getBranch().getId().equals(targetBranch.getId())) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN,
                    "Менеджер может импортировать только клиентов своего филиала");
        }
    }

    private void logProductHistory(ProductEntity product, ProductStatus status) {
        ProductHistoryEntity history = new ProductHistoryEntity();
        history.setProduct(product);
        history.setStatus(status);
        productHistoryRepository.save(history);
    }
}
