package com.cargoapp.backend.auth.service;

import com.cargoapp.backend.auth.config.JwtProperties;
import com.cargoapp.backend.auth.dto.*;
import com.cargoapp.backend.auth.entity.ConfirmationEntity;
import com.cargoapp.backend.auth.entity.ConfirmationStatus;
import com.cargoapp.backend.auth.entity.RefreshSessionEntity;
import com.cargoapp.backend.auth.event.ConfirmationEmailEvent;
import com.cargoapp.backend.auth.mapper.AuthMapper;
import com.cargoapp.backend.auth.repository.ConfirmationRepository;
import com.cargoapp.backend.auth.repository.RefreshSessionRepository;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.entity.UserPersonalCodeEntity;
import com.cargoapp.backend.users.entity.UserStatus;
import com.cargoapp.backend.users.repository.UserPersonalCodeRepository;
import com.cargoapp.backend.users.repository.UserRepository;
import com.cargoapp.backend.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchRepository branchRepository;
    private final UserPersonalCodeRepository userPersonalCodeRepository;
    private final ConfirmationRepository confirmationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionRepository refreshSessionRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Логин уже занят");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Email уже занят");
        }

        BranchEntity branch = branchRepository.findByIdForUpdate(request.branchId())
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));

        var role = userRoleRepository.findByRoleName("USER")
                .orElseThrow(() -> new AppException("INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Роль не найдена"));

        String personalCode = String.format("%s%04d", branch.getPersonalCodePrefix(), branch.getNextSequence());

        UserEntity user = new UserEntity();
        user.setLogin(request.login());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setDateOfBirth(request.dateOfBirth());
        user.setPersonalCode(personalCode);
        user.setBranch(branch);
        user.setRole(role);
        userRepository.save(user);

        UserPersonalCodeEntity personalCodeEntry = new UserPersonalCodeEntity();
        personalCodeEntry.setUser(user);
        personalCodeEntry.setPersonalCode(personalCode);
        personalCodeEntry.setBranch(branch);
        personalCodeEntry.setActive(true);
        userPersonalCodeRepository.save(personalCodeEntry);

        branch.setNextSequence(branch.getNextSequence() + 1);
        branchRepository.save(branch);

        String code = String.format("%04d", new SecureRandom().nextInt(10000));

        ConfirmationEntity confirmation = new ConfirmationEntity();
        confirmation.setUser(user);
        confirmation.setCode(code);
        confirmation.setConfirmationStatus(ConfirmationStatus.PENDING);
        confirmation.setAttempts(3);
        confirmation.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        confirmation.setLastSentAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);
        eventPublisher.publishEvent(new ConfirmationEmailEvent(user.getEmail(), code));
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }

        if (user.getStatus() == UserStatus.DELETED || user.getStatus() == UserStatus.PENDING_DELETION) {
            throw new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "Аккаунт заблокирован");
        }

        boolean emailNotConfirmed = confirmationRepository
                .findByUser_IdAndConfirmationStatus(user.getId(), ConfirmationStatus.PENDING)
                .isPresent();
        if (emailNotConfirmed) {
            throw new AppException("EMAIL_NOT_CONFIRMED", HttpStatus.FORBIDDEN, "Email не подтверждён");
        }

        String jti = UUID.randomUUID().toString();

        RefreshSessionEntity session = new RefreshSessionEntity();
        session.setUser(user);
        session.setJti(jti);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(
                jwtProperties.getRefreshExpirationMs() / 1000
        ));
        refreshSessionRepository.save(session);

        String accessToken = jwtService.generateAccessToken(
                user.getId(),
                user.getRole().getRoleName(),
                jti
        );
        String refreshToken = jwtService.generateRefreshToken(jti);

        return authMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Недействительный токен");
        }
        String jti = jwtService.extractRefreshClaims(refreshToken).getId();
        refreshSessionRepository.revokeByJti(jti, LocalDateTime.now());
    }

    @Transactional
    public TokenPairDto refreshToken(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Недействительный токен");
        }

        String jti = jwtService.extractRefreshClaims(refreshToken).getId();

        RefreshSessionEntity oldSession = refreshSessionRepository.findByJtiAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Сессия не найдена или отозвана"));

        refreshSessionRepository.revokeByJti(jti, LocalDateTime.now());

        UserEntity user = oldSession.getUser();
        String newJti = UUID.randomUUID().toString();

        RefreshSessionEntity newSession = new RefreshSessionEntity();
        newSession.setUser(user);
        newSession.setJti(newJti);
        newSession.setExpiresAt(LocalDateTime.now().plusSeconds(
                jwtProperties.getRefreshExpirationMs() / 1000
        ));
        refreshSessionRepository.save(newSession);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getRole().getRoleName(), newJti);
        String newRefreshToken = jwtService.generateRefreshToken(newJti);

        return new TokenPairDto(accessToken, newRefreshToken);
    }

    @Transactional
    public void confirm(ConfirmRequest request) {
        UserEntity user = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));

        ConfirmationEntity confirmation = confirmationRepository.findByUser_IdAndConfirmationStatus(user.getId(), ConfirmationStatus.PENDING)
                .orElseThrow(() -> new AppException("INVALID_CONFIRMATION_CODE", HttpStatus.BAD_REQUEST, "Нет активного кода подтверждения"));

        if (confirmation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("INVALID_CONFIRMATION_CODE", HttpStatus.BAD_REQUEST, "Код подтверждения истёк");
        }

        if (confirmation.getAttempts() <= 0) {
            throw new AppException("INVALID_CONFIRMATION_CODE", HttpStatus.BAD_REQUEST, "Превышено количество попыток");
        }

        if (!confirmation.getCode().equals(request.code())) {
            confirmation.setAttempts(confirmation.getAttempts() - 1);
            confirmationRepository.save(confirmation);
            throw new AppException("INVALID_CONFIRMATION_CODE", HttpStatus.BAD_REQUEST, "Неверный код подтверждения");
        }

        confirmation.setConfirmationStatus(ConfirmationStatus.VERIFIED);
        confirmationRepository.save(confirmation);
    }

    @Transactional
    public void resendConfirmation(String login) {
        UserEntity user = userRepository.findByLogin(login)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));

        ConfirmationEntity confirmation = confirmationRepository
                .findByUser_IdAndConfirmationStatus(user.getId(), ConfirmationStatus.PENDING)
                .orElseThrow(() -> new AppException("INVALID_CONFIRMATION_CODE", HttpStatus.BAD_REQUEST, "Нет активного кода подтверждения"));

        if (confirmation.getLastSentAt() != null &&
                confirmation.getLastSentAt().isAfter(LocalDateTime.now().minusSeconds(60))) {
            throw new AppException("RESEND_TOO_SOON", HttpStatus.TOO_MANY_REQUESTS, "Повторная отправка возможна через 60 секунд");
        }

        String newCode = String.format("%04d", new SecureRandom().nextInt(10000));
        confirmation.setCode(newCode);
        confirmation.setAttempts(3);
        confirmation.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        confirmation.setLastSentAt(LocalDateTime.now());
        confirmationRepository.save(confirmation);
        eventPublisher.publishEvent(new ConfirmationEmailEvent(user.getEmail(), newCode));
    }
}
