package com.cargoapp.backend.auth.mapper;

import com.cargoapp.backend.auth.dto.AuthResponse;
import com.cargoapp.backend.auth.dto.BranchResponse;
import com.cargoapp.backend.auth.dto.UserResponse;
import com.cargoapp.backend.auth.dto.WebAuthResponse;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.users.entity.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class AuthMapper {

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
          toBranchResponse(user.getBranch()),
          user.getStatus(),
          user.getCreatedAt().toString(),
          user.getUpdatedAt().toString()
        );
    }

    public BranchResponse toBranchResponse(BranchEntity branch) {
        if (branch == null) return null;
        return new BranchResponse(
                branch.getId(),
                branch.getAddress(),
                branch.getPersonalCodePrefix(),
                branch.isActive()
        );
    }

    public AuthResponse toAuthResponse(UserEntity user, String accessToken, String refreshToken) {
        return new AuthResponse(
                accessToken,
                refreshToken,
                toUserResponse(user)
        );
    }

    public WebAuthResponse toWebAuthresponse(UserEntity user) {
        return new WebAuthResponse(true, toUserResponse(user));
    }
}
