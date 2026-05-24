package com.cargoapp.backend.auth.service;

import com.cargoapp.backend.auth.config.JwtProperties;
import com.cargoapp.backend.common.constants.ClientType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final JwtProperties jwtProperties;
    private final SecretKey accessKey;
    private final SecretKey refreshKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.accessKey = Keys.hmacShaKeyFor(jwtProperties.getAccessSecret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(jwtProperties.getRefreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Генерирует access-токен для мобильного пользователя (user).
     * claim type = "user", role не включается.
     */
    public String generateUserAccessToken(UUID userId, String jti) {
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", ClientType.USER.getValue())
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpirationMs()))
                .signWith(accessKey)
                .compact();
    }

    /**
     * Генерирует access-токен для менеджера (web).
     * claim type = "manager", claim role = MANAGER | SUPER_ADMIN.
     */
    public String generateManagerAccessToken(UUID managerId, String role, String jti) {
        return Jwts.builder()
                .subject(managerId.toString())
                .claim("type", ClientType.MANAGER.getValue())
                .claim("role", role)
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessExpirationMs()))
                .signWith(accessKey)
                .compact();
    }

    /**
     * @deprecated Используй generateUserAccessToken или generateManagerAccessToken.
     * Оставлен для обратной совместимости на период рефакторинга.
     */
    @Deprecated
    public String generateAccessToken(UUID userId, String role, String jti) {
        return generateManagerAccessToken(userId, role, jti);
    }

    public String generateRefreshToken(String jti) {
        return Jwts.builder()
                .id(jti)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getRefreshExpirationMs()))
                .signWith(refreshKey)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        try {
            Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractAccessClaims(String token) {
        return Jwts.parser().verifyWith(accessKey).build().parseSignedClaims(token).getPayload();
    }

    public boolean validateRefreshToken(String token) {
        try {
            Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Claims extractRefreshClaims(String token) {
        return Jwts.parser().verifyWith(refreshKey).build().parseSignedClaims(token).getPayload();
    }
}
