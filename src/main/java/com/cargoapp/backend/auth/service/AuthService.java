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
import com.cargoapp.backend.managers.dto.ManagerResponse;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import com.cargoapp.backend.managers.entity.ManagerRefreshSessionEntity;
import com.cargoapp.backend.managers.mapper.ManagerMapper;
import com.cargoapp.backend.managers.repository.ManagerRefreshSessionRepository;
import com.cargoapp.backend.managers.repository.ManagerRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.entity.UserPersonalCodeEntity;
import com.cargoapp.backend.users.entity.UserStatus;
import com.cargoapp.backend.users.repository.UserPersonalCodeRepository;
import com.cargoapp.backend.users.repository.UserRepository;
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
    private final BranchRepository branchRepository;
    private final UserPersonalCodeRepository userPersonalCodeRepository;
    private final ConfirmationRepository confirmationRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshSessionRepository refreshSessionRepository;
    private final ManagerRepository managerRepository;
    private final ManagerRefreshSessionRepository managerRefreshSessionRepository;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthMapper authMapper;
    private final ManagerMapper managerMapper;
    private final ApplicationEventPublisher eventPublisher;

    // =============================================
    // REGISTER (только мобильные пользователи)
    // =============================================

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

    // =============================================
    // LOGIN — мобильный клиент (X-Client-Type: mobile)
    // =============================================

    @Transactional
    public AuthResponse loginUser(LoginRequest request) {
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
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000));
        refreshSessionRepository.save(session);

        String accessToken = jwtService.generateUserAccessToken(user.getId(), jti);
        String refreshToken = jwtService.generateRefreshToken(jti);

        return authMapper.toAuthResponse(user, accessToken, refreshToken);
    }

    // =============================================
    // LOGIN — веб-клиент (X-Client-Type: web)
    // =============================================

    @Transactional
    public ManagerLoginResult loginManager(LoginRequest request) {
        ManagerEntity manager = managerRepository.findByLogin(request.login())
                .orElseThrow(() -> new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный логин или пароль"));

        if (!passwordEncoder.matches(request.password(), manager.getPasswordHash())) {
            throw new AppException("INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Неверный логин или пароль");
        }

        String jti = UUID.randomUUID().toString();

        ManagerRefreshSessionEntity session = new ManagerRefreshSessionEntity();
        session.setManager(manager);
        session.setJti(jti);
        session.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000));
        managerRefreshSessionRepository.save(session);

        String accessToken = jwtService.generateManagerAccessToken(manager.getId(), manager.getRole(), jti);
        String refreshToken = jwtService.generateRefreshToken(jti);

        return new ManagerLoginResult(accessToken, refreshToken, managerMapper.toManagerResponse(manager));
    }

    // =============================================
    // LOGOUT
    // =============================================

    @Transactional
    public void logout(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Недействительный токен");
        }
        String jti = jwtService.extractRefreshClaims(refreshToken).getId();

        // Пробуем отозвать в обоих хранилищах — один из запросов найдёт запись
        refreshSessionRepository.revokeByJti(jti, LocalDateTime.now());
        managerRefreshSessionRepository.revokeByJti(jti, LocalDateTime.now());
    }

    // =============================================
    // REFRESH TOKEN
    // =============================================

    @Transactional
    public TokenPairDto refreshUserToken(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Недействительный токен");
        }

        String jti = jwtService.extractRefreshClaims(refreshToken).getId();

        RefreshSessionEntity oldSession = refreshSessionRepository.findByJtiAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Сессия не найдена или отозвана"));

        if (oldSession.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Сессия истекла");
        }

        refreshSessionRepository.revokeByJti(jti, LocalDateTime.now());

        UserEntity user = oldSession.getUser();
        String newJti = UUID.randomUUID().toString();

        RefreshSessionEntity newSession = new RefreshSessionEntity();
        newSession.setUser(user);
        newSession.setJti(newJti);
        newSession.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000));
        refreshSessionRepository.save(newSession);

        String newAccessToken = jwtService.generateUserAccessToken(user.getId(), newJti);
        String newRefreshToken = jwtService.generateRefreshToken(newJti);

        return new TokenPairDto(newAccessToken, newRefreshToken);
    }

    @Transactional
    public TokenPairDto refreshManagerToken(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Недействительный токен");
        }

        String jti = jwtService.extractRefreshClaims(refreshToken).getId();

        ManagerRefreshSessionEntity oldSession = managerRefreshSessionRepository.findByJtiAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Сессия не найдена или отозвана"));

        if (oldSession.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Сессия истекла");
        }

        managerRefreshSessionRepository.revokeByJti(jti, LocalDateTime.now());

        ManagerEntity manager = oldSession.getManager();
        String newJti = UUID.randomUUID().toString();

        ManagerRefreshSessionEntity newSession = new ManagerRefreshSessionEntity();
        newSession.setManager(manager);
        newSession.setJti(newJti);
        newSession.setExpiresAt(LocalDateTime.now().plusSeconds(jwtProperties.getRefreshExpirationMs() / 1000));
        managerRefreshSessionRepository.save(newSession);

        String newAccessToken = jwtService.generateManagerAccessToken(manager.getId(), manager.getRole(), newJti);
        String newRefreshToken = jwtService.generateRefreshToken(newJti);

        return new TokenPairDto(newAccessToken, newRefreshToken);
    }

    // =============================================
    // CONFIRM / RESEND (только мобильные пользователи)
    // =============================================

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

    // =============================================
    // Внутренний record для результата loginManager
    // =============================================

    public record ManagerLoginResult(
            String accessToken,
            String refreshToken,
            ManagerResponse manager
    ) {}
}
