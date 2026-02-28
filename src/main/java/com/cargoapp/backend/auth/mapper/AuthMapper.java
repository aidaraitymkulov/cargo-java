package com.cargoapp.backend.auth.mapper;

import com.cargoapp.backend.auth.dto.AuthResponse;
import com.cargoapp.backend.auth.dto.UserResponse;
import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.users.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthMapper {

    private final BranchMapper branchMapper;

    public UserResponse toUserResponse(UserEntity user) {
        return new UserResponse(
          user.getId(),
          user.getEmail(),
          user.getFirstName(),
          user.getLastName(),
          user.getPhone(),
          user.getDateOfBirth(),
          user.getPersonalCode(),
          user.getRole().getRoleName(),
          branchMapper.toBranchResponse(user.getBranch()),
          user.getStatus().name(),
          user.getCreatedAt(),
          user.getUpdatedAt()
        );
    }

    public AuthResponse toAuthResponse(UserEntity user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                toUserResponse(user)
        );
    }
}
