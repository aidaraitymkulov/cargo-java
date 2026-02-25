package com.cargoapp.backend.auth.service;

import com.cargoapp.backend.auth.config.JwtProperties;
import com.cargoapp.backend.auth.dto.AuthResponse;
import com.cargoapp.backend.auth.dto.LoginRequest;
import com.cargoapp.backend.auth.dto.RegisterRequest;
import com.cargoapp.backend.auth.dto.TokenPairDto;
import com.cargoapp.backend.auth.mapper.AuthMapper;
import com.cargoapp.backend.auth.entity.ConfirmationEntity;
import com.cargoapp.backend.auth.entity.ConfirmationStatus;
import com.cargoapp.backend.auth.entity.RefreshSessionEntity;
import com.cargoapp.backend.auth.repository.ConfirmationRepository;
import com.cargoapp.backend.auth.repository.RefreshSessionRepository;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.entity.UserPersonalCodeEntity;
import com.cargoapp.backend.users.repository.UserPersonalCodeRepository;
import com.cargoapp.backend.users.repository.UserRepository;
import com.cargoapp.backend.users.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
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

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByLogin(request.login())) {
            throw new RuntimeException("CONFLICT");
        }
        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("CONFLICT");
        }

        BranchEntity branch = branchRepository.findById(request.branchId())
                .orElseThrow(() -> new RuntimeException("BRANCH_NOT_FOUND"));

        var role = userRoleRepository.findByRoleName("USER")
                .orElseThrow(() -> new RuntimeException("ROLE_NOT_FOUND"));

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
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByLogin(request.login())
                .orElseThrow(() -> new RuntimeException("INVALID_CREDENTIALS"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("INVALID_CREDENTIALS");
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
        String jti = jwtService.extractRefreshClaims(refreshToken).getId();
        refreshSessionRepository.revokeByJti(jti, LocalDateTime.now());
    }

    @Transactional
    public TokenPairDto refreshToken(String refreshToken) {
        if (!jwtService.validateRefreshToken(refreshToken)) {
            throw new RuntimeException("INVALID_TOKEN");
        }

        String jti = jwtService.extractRefreshClaims(refreshToken).getId();

        RefreshSessionEntity oldSession = refreshSessionRepository.findByJtiAndRevokedAtIsNull(jti)
                .orElseThrow(() -> new RuntimeException("INVALID_TOKEN"));

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
}
