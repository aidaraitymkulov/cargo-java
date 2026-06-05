package com.cargoapp.backend.users.service;

import com.cargoapp.backend.auth.repository.RefreshSessionRepository;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.component.BranchResolver;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.users.dto.*;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.entity.UserPersonalCodeEntity;
import com.cargoapp.backend.users.entity.UserStatus;
import com.cargoapp.backend.users.mapper.UserMapper;
import com.cargoapp.backend.users.repository.UserPersonalCodeRepository;
import com.cargoapp.backend.users.repository.UserRepository;
import com.cargoapp.backend.users.repository.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final BranchRepository branchRepository;
    private final UserPersonalCodeRepository userPersonalCodeRepository;
    private final RefreshSessionRepository refreshSessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final BranchResolver branchResolver;

    public UserResponse getMe(UUID userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @Transactional
    public UserResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        UserEntity user = findUserById(userId);

        if (request.email() != null && !request.email().equals(user.getEmail())) {
            if (userRepository.existsByEmail(request.email())) {
                throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Email уже занят");
            }
            user.setEmail(request.email());
        }

        if (request.firstName() != null) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        UserEntity user = findUserById(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный текущий пароль");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public UserResponse changeBranch(UUID userId, ChangeBranchRequest request) {
        UserEntity user = findUserById(userId);

        BranchEntity branch = branchRepository.findByIdForUpdate(request.branchId())
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));

        List<UserPersonalCodeEntity> activeCodes = userPersonalCodeRepository.findAllByUser_Id(userId)
                .stream()
                .filter(UserPersonalCodeEntity::isActive)
                .toList();

        activeCodes.forEach(code -> code.setActive(false));
        userPersonalCodeRepository.saveAll(activeCodes);

        String newPersonalCode = String.format("%s%04d", branch.getPersonalCodePrefix(), branch.getNextSequence());

        UserPersonalCodeEntity newCode = new UserPersonalCodeEntity();
        newCode.setUser(user);
        newCode.setPersonalCode(newPersonalCode);
        newCode.setBranch(branch);
        newCode.setActive(true);
        userPersonalCodeRepository.save(newCode);

        branch.setNextSequence(branch.getNextSequence() + 1);
        branchRepository.save(branch);

        user.setPersonalCode(newPersonalCode);
        user.setBranch(branch);

        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public DeletionResponseDto requestDeletion(UUID userId) {
        UserEntity user = findUserById(userId);

        if (user.getStatus() == UserStatus.PENDING_DELETION) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Аккаунт уже помечен на удаление");
        }

        user.setStatus(UserStatus.PENDING_DELETION);
        userRepository.save(user);

        refreshSessionRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());

        return new DeletionResponseDto(true, LocalDateTime.now().plusDays(30));
    }

    @Transactional
    public void cancelDeletion(UUID userId) {
        UserEntity user = findUserById(userId);

        if (user.getStatus() != UserStatus.PENDING_DELETION) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Аккаунт не находится в статусе ожидания удаления");
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    public PagedResponse<UserResponse> searchUsers(UUID currentManagerId, String query, int page, int pageSize) {
        if (query == null || query.isBlank()) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Параметр поиска не может быть пустым");
        }
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, null);
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        var spec = UserSpecification.search(query, effectiveBranchId);
        Page<UserEntity> result = userRepository.findAll(spec, pageable);

        List<UserResponse> items = result.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }

    public PagedResponse<UserResponse> getUsers(UUID currentManagerId, String prefix, String code, UUID branchId, int page, int pageSize) {
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, branchId);
        var pageable = PageRequest.of(page - 1, pageSize, Sort.by("createdAt").descending());
        var spec = UserSpecification.filter(prefix, code, effectiveBranchId);
        Page<UserEntity> result = userRepository.findAll(spec, pageable);

        List<UserResponse> items = result.getContent().stream()
                .map(userMapper::toUserResponse)
                .toList();

        return new PagedResponse<>(items, page, pageSize, result.getTotalElements());
    }

    public UserStatsResponse getUserStats(UUID currentManagerId, UUID branchId) {
        UUID effectiveBranchId = branchResolver.resolveForManager(currentManagerId, branchId);

        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        long total;
        long newThisMonth;

        if (effectiveBranchId != null) {
            total = userRepository.countUsersByBranch(effectiveBranchId);
            newThisMonth = userRepository.countNewUsersByBranch(effectiveBranchId, monthStart, monthEnd);
        } else {
            total = userRepository.countAllUsers();
            newThisMonth = userRepository.countNewUsers(monthStart, monthEnd);
        }

        return new UserStatsResponse(total, newThisMonth);
    }

    public UserResponse getUserById(UUID userId) {
        return userMapper.toUserResponse(findUserById(userId));
    }

    @Transactional
    public void deleteUser(UUID userId) {
        UserEntity user = findUserById(userId);
        user.setStatus(UserStatus.DELETED);
        userRepository.save(user);
        refreshSessionRepository.revokeAllActiveByUserId(userId, LocalDateTime.now());
    }

    @Transactional
    public UserResponse chatBan(UUID userId) {
        UserEntity user = findUserById(userId);
        user.setChatBanned(true);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse chatUnban(UUID userId) {
        UserEntity user = findUserById(userId);
        user.setChatBanned(false);
        return userMapper.toUserResponse(userRepository.save(user));
    }

    private UserEntity findUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));
    }
}
