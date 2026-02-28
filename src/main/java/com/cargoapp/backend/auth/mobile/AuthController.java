package com.cargoapp.backend.auth.mobile;

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

    @PostMapping("/login")
    public Object login(
            @Valid @RequestBody LoginRequest request,
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            HttpServletResponse response
    ) {
        AuthResponse auth = authService.login(request);

        if (isWebClient(clientType)) {
            setTokenCookies(response, auth.accessToken(), auth.refreshToken());
            return auth.user();
        }

        return auth;
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
            String refreshToken = extractCookie(request, "refreshToken");
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

    @PostMapping("/refresh")
    public Object refresh(
            @RequestHeader(value = "X-Client-Type", required = false) String clientType,
            @RequestBody(required = false) RefreshRequest body,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (isWebClient(clientType)) {
            String refreshToken = extractCookie(request, "refreshToken");
            if (refreshToken == null) {
                throw new AppException("INVALID_TOKEN", HttpStatus.UNAUTHORIZED, "Refresh токен отсутствует");
            }
            TokenPairDto tokens = authService.refreshToken(refreshToken);
            setTokenCookies(response, tokens.accessToken(), tokens.refreshToken());
            response.setStatus(HttpStatus.NO_CONTENT.value());
            return null;
        }

        if (body == null || body.refreshToken() == null || body.refreshToken().isBlank()) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "refreshToken обязателен");
        }
        return authService.refreshToken(body.refreshToken());
    }

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

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private boolean isWebClient(String clientType) {
        return "web".equalsIgnoreCase(clientType);
    }
}
