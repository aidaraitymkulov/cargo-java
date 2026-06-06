package com.cargoapp.backend.config;

import com.cargoapp.backend.auth.service.JwtService;
import com.cargoapp.backend.common.constants.ClientType;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;

    @Override
    public @Nullable Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor != null && StompCommand.CONNECT.equals(accessor.getCommand())) {
            String token = extractToken(accessor);

            if (token != null && jwtService.validateAccessToken(token)) {
                Claims claims = jwtService.extractAccessClaims(token);
                String subject = claims.getSubject();
                String type = claims.get("type", String.class);

                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (ClientType.MANAGER.getValue().equals(type)) {
                    String role = claims.get("role", String.class);
                    if (role != null) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                }

                var authentication = new UsernamePasswordAuthenticationToken(subject, null, authorities);
                accessor.setUser(authentication);
            } else {
                throw new MessageDeliveryException("UNAUTHORIZED");
            }
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        String token = accessor.getFirstNativeHeader("token");
        if (token != null) {
            return token;
        }

        Map<String, Object> sessionAttrs = accessor.getSessionAttributes();
        if (sessionAttrs != null) {
            Object cookieToken = sessionAttrs.get("accessToken");
            if (cookieToken != null) {
                return cookieToken.toString();
            }
        }

        return null;
    }
}