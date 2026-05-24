package com.cargoapp.backend.managers.service;

import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.managers.dto.CreateManagerRequest;
import com.cargoapp.backend.managers.dto.ManagerResponse;
import com.cargoapp.backend.managers.dto.UpdateManagerRequest;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.mapper.ManagerMapper;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import com.cargoapp.backend.common.constants.ManagerRole;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ManagerService {

    private final ManagerRepository managerRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final ManagerMapper managerMapper;

    @Transactional
    public ManagerResponse createManager(CreateManagerRequest request) {
        if (managerRepository.existsByLogin(request.login())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Логин уже занят");
        }

        BranchEntity branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));

        ManagerEntity manager = new ManagerEntity();
        manager.setLogin(request.login());
        manager.setPasswordHash(passwordEncoder.encode(request.password()));
        manager.setFirstName(request.firstName());
        manager.setLastName(request.lastName());
        manager.setPhone(request.phone());
        manager.setRole(ManagerRole.MANAGER.name());
        manager.setBranch(branch);

        return managerMapper.toManagerResponse(managerRepository.save(manager));
    }

    public List<ManagerResponse> getManagers() {
        return managerRepository.findAllByRoleNot(ManagerRole.SUPER_ADMIN.name(), Sort.by("createdAt").descending())
                .stream()
                .map(managerMapper::toManagerResponse)
                .toList();
    }

    public ManagerResponse getManagerById(UUID managerId) {
        return managerMapper.toManagerResponse(findManagerById(managerId));
    }

    @Transactional
    public ManagerResponse updateManager(UUID managerId, UpdateManagerRequest request) {
        ManagerEntity manager = findManagerById(managerId);

        if (ManagerRole.SUPER_ADMIN.name().equals(manager.getRole())) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нельзя редактировать данные супер-администратора");
        }

        if (request.login() != null && !request.login().equals(manager.getLogin())) {
            if (managerRepository.existsByLogin(request.login())) {
                throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Логин уже занят");
            }
            manager.setLogin(request.login());
        }

        if (request.password() != null && !request.password().isBlank()) {
            manager.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        if (request.firstName() != null) {
            manager.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            manager.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            manager.setPhone(request.phone());
        }

        if (request.branchId() != null) {
            BranchEntity branch = branchRepository.findById(request.branchId())
                    .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));
            manager.setBranch(branch);
        }

        return managerMapper.toManagerResponse(managerRepository.save(manager));
    }

    @Transactional
    public void deleteManager(UUID managerId, UUID currentManagerId) {
        if (managerId.equals(currentManagerId)) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Нельзя удалить себя");
        }

        ManagerEntity manager = findManagerById(managerId);

        if (ManagerRole.SUPER_ADMIN.name().equals(manager.getRole())) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Нельзя удалить супер-администратора");
        }

        managerRepository.deleteById(managerId);
    }

    private ManagerEntity findManagerById(UUID managerId) {
        return managerRepository.findById(managerId)
                .orElseThrow(() -> new AppException("NOT_FOUND", HttpStatus.NOT_FOUND, "Менеджер не найден"));
    }
}
