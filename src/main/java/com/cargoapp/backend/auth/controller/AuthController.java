package com.cargoapp.backend.auth.controller;

import com.cargoapp.backend.auth.config.JwtProperties;
import com.cargoapp.backend.auth.dto.*;
import com.cargoapp.backend.auth.service.AuthService;
import com.cargoapp.backend.common.exception.AppException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
    }

    /**
     * Единый логин.
     * X-Client-Type: mobile → ищем в users, возвращаем токены + UserResponse в теле
     * X-Client-Type: web   → ищем в managers, токены в HttpOnly cookies, в теле только ManagerResponse
     */
    @PostMapping("/login")
    public Object login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse response
    ) {
        if (isWebClient(clientType)) {
            AuthService.ManagerLoginResult result = authService.loginManager(request);
            setTokenCookies(response, result.accessToken(), result.refreshToken());
            return result.manager();
        }

        // mobile (default)
        return authService.loginUser(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestBody(required = false) LogoutRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (isWebClient(clientType)) {
            String refreshToken = extractRefreshCookie(request);
            if (refreshToken != null) {
                authService.logout(refreshToken);
            }
            clearTokenCookies(response);
        } else {
            if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
                throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "refreshToken обязателен");
            }
            authService.logout(body.refreshToken());
        }
    }

    /**
     * Обновление токена.
     * X-Client-Type: web   → refresh из cookie, ищем в manager_refresh_sessions
     * X-Client-Type: mobile → refresh из тела, ищем в refresh_sessions
     */
    @PostMapping("/refresh")
    public Object refresh(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (isWebClient(clientType)) {
            String refreshToken = extractRefreshCookie(request);
            if (refreshToken == null) {
                throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh токен отсутствует");
            }
            TokenPairDto tokens = authService.refreshManagerToken(refreshToken);
            setTokenCookies(response, tokens.accessToken(), tokens.refreshToken());
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;
        }

        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "refreshToken обязателен");
        }
        return authService.refreshUserToken(body.refreshToken());
    }

    @PostMapping("/confirm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirm(@Valid @RequestBody ConfirmRequest request) {
        authService.confirm(request);
    }

    @PostMapping("/resend")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resend(@RequestParam String login) {
        authService.resendConfirmation(login);
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
    }

    @PostMapping("/forgot-password/verify")
    public ResponseEntity<ResetTokenResponse> verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return ResponseEntity.ok(authService.verifyResetCode(request));
    }

    @PostMapping("/forgot-password/reset")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
    }

    // =============================================
    // Вспомогательные методы для cookie
    // =============================================

    private void setTokenCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        response.addHeader("Set-Cookie", buildCookie("accessToken", accessToken,
                jwtProperties.getAccessExpirationMs() / 1000).toString());
        response.addHeader("Set-Cookie", buildCookie("refreshToken", refreshToken,
                jwtProperties.getRefreshExpirationMs() / 1000).toString());
    }

    private void clearTokenCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildCookie("accessToken", "", 0).toString());
        response.addHeader("Set-Cookie", buildCookie("refreshToken", "", 0).toString());
    }

    private ResponseCookie buildCookie(String name, String value, long maxAgeSeconds) {
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(jwtProperties.isCookieSecure())
                .sameSite(jwtProperties.getCookieSameSite())
                .path("/")
                .maxAge(Duration.ofSeconds(maxAgeSeconds))
                .build();
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> "refreshToken".equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isWebClient(String clientType) {
        return "web".equalsIgnoreCase(clientType);
    }
}
