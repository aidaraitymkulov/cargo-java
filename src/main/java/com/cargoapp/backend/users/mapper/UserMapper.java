package com.cargoapp.backend.users.mapper;

import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.users.dto.UserResponse;
import com.cargoapp.backend.users.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserMapper {

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
}
