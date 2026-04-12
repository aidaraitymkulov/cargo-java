package com.cargoapp.backend.common.component;

import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Определяет эффективный branchId для запросов с фильтрацией по филиалу.
 *
 * MANAGER → всегда его собственный филиал, запрошенный branchId игнорируется.
 * SUPER_ADMIN → может запросить любой филиал (или null = все).
 */
@Component
@RequiredArgsConstructor
public class BranchResolver {

    private final ManagerRepository managerRepository;

    /**
     * Разрешает branchId для менеджера по его managerId из JWT.
     *
     * @param currentManagerId UUID из JWT claim sub (тип = manager)
     * @param requestedBranchId branchId из query-параметра (только для SUPER_ADMIN)
     * @return UUID филиала или null (SUPER_ADMIN без фильтра)
     */
    public UUID resolveForManager(UUID currentManagerId, UUID requestedBranchId) {
        ManagerEntity manager = managerRepository.findById(currentManagerId)
                .orElseThrow(() -> new AppException("NOT_FOUND", HttpStatus.NOT_FOUND, "Менеджер не найден"));

        if ("MANAGER".equals(manager.getRole())) {
            if (manager.getBranch() == null) {
                throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "У менеджера не назначен филиал");
            }
            return manager.getBranch().getId();
        }

        // SUPER_ADMIN — возвращаем запрошенный (может быть null = без фильтра)
        return requestedBranchId;
    }
}
