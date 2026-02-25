package com.cargoapp.backend.auth.admin;

import com.cargoapp.backend.auth.config.JwtProperties;
import com.cargoapp.backend.auth.dto.AuthResponse;
import com.cargoapp.backend.auth.dto.LoginRequest;
import com.cargoapp.backend.auth.dto.TokenPairDto;
import com.cargoapp.backend.auth.dto.WebAuthResponse;
import com.cargoapp.backend.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.util.Arrays;

@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @PostMapping("/login")
    public WebAuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse auth = authService.login(request);
        setTokenCookies(response, auth.accessToken(), auth.refreshToken());
        return new WebAuthResponse(auth.user());
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null) {
            throw new RuntimeException("INVALID_TOKEN");
        }
        TokenPairDto tokens = authService.refreshToken(refreshToken);
        setTokenCookies(response, tokens.accessToken(), tokens.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        clearTokenCookies(response);
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
}
