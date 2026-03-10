package com.cargoapp.backend.imports.service;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.imports.dto.ImportErrorDto;
import com.cargoapp.backend.imports.dto.ImportResultResponse;
import com.cargoapp.backend.imports.parser.ExcelImportParser;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Сервис KG-импорта — оркестрирует обработку блоков.
 *
 * Намеренно НЕ помечен @Transactional на уровне класса.
 * Каждый блок обрабатывается в отдельной транзакции через KgBlockProcessor
 * (там REQUIRES_NEW). Это как Promise.allSettled() — каждый блок независим.
 *
 * Почему KgBlockProcessor вынесен отдельно:
 * Spring AOP-прокси работает только при вызове метода ЧЕРЕЗ прокси (через другой бин).
 * Если вызывать processBlock() внутри того же класса (this.processBlock()),
 * Spring проксирование не сработает и @Transactional(REQUIRES_NEW) будет проигнорирован.
 * Это классическая ловушка — аналог проблемы с обычным 'this' vs Proxy в NestJS.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgImportService {

    private final ExcelImportParser parser;
    private final BranchRepository branchRepository;
    private final UserRepository userRepository;
    private final KgBlockProcessor blockProcessor;

    // =========================================================================
    // 1. Импорт "после сортировки" — in-kg
    // =========================================================================

    public ImportResultResponse importInKg(MultipartFile file,
                                           String prefix,
                                           UUID currentUserId,
                                           boolean isManager) {
        List<ExcelImportParser.KgBlock> blocks = parseFile(file);

        BranchEntity branch = resolveBranch(prefix);
        if (isManager) {
            validateManagerCanAccessBranch(currentUserId, branch);
        }

        int imported = 0;
        List<ImportErrorDto> errors = new ArrayList<>();

        for (ExcelImportParser.KgBlock block : blocks) {
            try {
                // Вызываем через blockProcessor (другой бин) — прокси работает, REQUIRES_NEW создаётся
                blockProcessor.processInKgBlock(block, branch);
                imported++;
            } catch (AppException ex) {
                errors.add(new ImportErrorDto(block.summaryRowNumber(), ex.getErrorCode(), ex.getMessage()));
                log.warn("in-kg block row={} skipped: {} — {}", block.summaryRowNumber(), ex.getErrorCode(), ex.getMessage());
            } catch (Exception ex) {
                errors.add(new ImportErrorDto(block.summaryRowNumber(), "IMPORT_ERROR", ex.getMessage()));
                log.error("in-kg block row={} unexpected error", block.summaryRowNumber(), ex);
            }
        }

        log.info("KG in-kg import done: prefix={}, imported={}, errors={}", prefix, imported, errors.size());
        return new ImportResultResponse(imported, errors);
    }

    // =========================================================================
    // 2. Импорт выдачи — delivered
    // =========================================================================

    public ImportResultResponse importDelivered(MultipartFile file,
                                                String prefix,
                                                UUID currentUserId,
                                                boolean isManager) {
        List<ExcelImportParser.KgBlock> blocks = parseFile(file);

        BranchEntity branch = resolveBranch(prefix);
        if (isManager) {
            validateManagerCanAccessBranch(currentUserId, branch);
        }

        int imported = 0;
        List<ImportErrorDto> errors = new ArrayList<>();

        for (ExcelImportParser.KgBlock block : blocks) {
            try {
                blockProcessor.processDeliveredBlock(block, branch);
                imported++;
            } catch (AppException ex) {
                errors.add(new ImportErrorDto(block.summaryRowNumber(), ex.getErrorCode(), ex.getMessage()));
                log.warn("delivered block row={} skipped: {} — {}", block.summaryRowNumber(), ex.getErrorCode(), ex.getMessage());
            } catch (Exception ex) {
                errors.add(new ImportErrorDto(block.summaryRowNumber(), "IMPORT_ERROR", ex.getMessage()));
                log.error("delivered block row={} unexpected error", block.summaryRowNumber(), ex);
            }
        }

        log.info("KG delivered import done: prefix={}, imported={}, errors={}", prefix, imported, errors.size());
        return new ImportResultResponse(imported, errors);
    }

    // =========================================================================
    // Вспомогательные методы
    // =========================================================================

    private List<ExcelImportParser.KgBlock> parseFile(MultipartFile file) {
        try {
            return parser.parseKgFile(file);
        } catch (IOException e) {
            log.error("Failed to parse KG import file", e);
            throw new AppException("IMPORT_ERROR", HttpStatus.BAD_REQUEST, "Не удалось прочитать файл Excel");
        }
    }

    private BranchEntity resolveBranch(String prefix) {
        return branchRepository.findByPersonalCodePrefix(prefix)
                .orElseThrow(() -> new AppException(
                        "BRANCH_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Филиал с prefix=" + prefix + " не найден"
                ));
    }

    private void validateManagerCanAccessBranch(UUID managerId, BranchEntity targetBranch) {
        UserEntity manager = userRepository.findById(managerId)
                .orElseThrow(() -> new AppException(
                        "USER_NOT_FOUND",
                        HttpStatus.NOT_FOUND,
                        "Пользователь не найден"
                ));

        if (manager.getBranch() == null) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Менеджер не привязан к филиалу");
        }

        if (!manager.getBranch().getId().equals(targetBranch.getId())) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN,
                    "Менеджер может импортировать только файлы своего филиала");
        }
    }
}
