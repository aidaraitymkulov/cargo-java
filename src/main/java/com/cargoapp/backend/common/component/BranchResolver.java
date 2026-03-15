package com.cargoapp.backend.common.component;

import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class BranchResolver {

    private final UserRepository userRepository;

    public UUID resolve(UUID currentUserId, UUID requestedBranchId) {
        var user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException("USER_NOT_FOUND", HttpStatus.NOT_FOUND, "Пользователь не найден"));

        if ("MANAGER".equals(user.getRole().getRoleName())) {
            if (user.getBranch() == null) {
                throw new AppException("FORBIDDEN", HttpStatus.FORBIDDEN, "У менеджера не назначен филиал");
            }
            return user.getBranch().getId();
        }

        return requestedBranchId;
    }
}
