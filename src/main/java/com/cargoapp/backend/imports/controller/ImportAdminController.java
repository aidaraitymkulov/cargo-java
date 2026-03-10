package com.cargoapp.backend.imports.controller;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.imports.dto.ImportResultResponse;
import com.cargoapp.backend.imports.service.ChinaImportService;
import com.cargoapp.backend.imports.service.KgImportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Контроллер импорта Excel-файлов.
 *
 * Принимает multipart/form-data — это как multer в Express/NestJS.
 * @RequestParam("file") MultipartFile = req.file в Express.
 *
 * Права:
 * - MANAGER: только свой филиал (проверка внутри сервиса)
 * - SUPER_ADMIN: любой филиал
 */
@RestController
@RequestMapping("/admin/import")
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class ImportAdminController {

    private final ChinaImportService chinaImportService;
    private final KgImportService kgImportService;

    /**
     * POST /admin/import/parcels/cn
     * Импорт посылок из Китая.
     *
     * Body: multipart/form-data, поле "file" — .xlsx файл
     */
    @PostMapping(value = "/parcels/cn", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importFromChina(
            @RequestParam("file") MultipartFile file,
            @CurrentUserId UUID currentUserId,
            Authentication authentication
    ) {
        boolean isManager = isManager(authentication);
        ImportResultResponse result = chinaImportService.importFromChina(file, currentUserId, isManager);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /admin/import/parcels/kg/{prefix}/in-kg
     * Импорт после сортировки (создание заказов).
     *
     * {prefix} — код филиала, например "AN", "D", "OW"
     */
    @PostMapping(value = "/parcels/kg/{prefix}/in-kg", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importInKg(
            @PathVariable String prefix,
            @RequestParam("file") MultipartFile file,
            @CurrentUserId UUID currentUserId,
            Authentication authentication
    ) {
        boolean isManager = isManager(authentication);
        ImportResultResponse result = kgImportService.importInKg(file, prefix, currentUserId, isManager);
        return ResponseEntity.ok(result);
    }

    /**
     * POST /admin/import/parcels/kg/{prefix}/delivered
     * Импорт выдачи (перевод заказов в DELIVERED).
     */
    @PostMapping(value = "/parcels/kg/{prefix}/delivered", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ImportResultResponse> importDelivered(
            @PathVariable String prefix,
            @RequestParam("file") MultipartFile file,
            @CurrentUserId UUID currentUserId,
            Authentication authentication
    ) {
        boolean isManager = isManager(authentication);
        ImportResultResponse result = kgImportService.importDelivered(file, prefix, currentUserId, isManager);
        return ResponseEntity.ok(result);
    }

    private boolean isManager(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_MANAGER".equals(a.getAuthority()));
    }
}
