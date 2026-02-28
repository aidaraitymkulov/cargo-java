package com.cargoapp.backend.auth.mapper;

import com.cargoapp.backend.auth.dto.AuthResponse;
import com.cargoapp.backend.users.dto.UserResponse;
import com.cargoapp.backend.users.entity.UserEntity;
import com.cargoapp.backend.users.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final UserMapper userMapper;

    public UserResponse toUserResponse(UserEntity user) {
        return userMapper.toUserResponse(user);
    }

    public AuthResponse toAuthResponse(UserEntity user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                toUserResponse(user)
        );
    }
}
