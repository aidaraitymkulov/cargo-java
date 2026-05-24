package com.cargoapp.backend.managers.mapper;

import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.managers.dto.ManagerResponse;
import com.cargoapp.backend.managers.entity.ManagerEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ManagerMapper {

    private final BranchMapper branchMapper;

    public ManagerResponse toManagerResponse(ManagerEntity manager) {
        return new ManagerResponse(
                manager.getId(),
                manager.getLogin(),
                manager.getFirstName(),
                manager.getLastName(),
                manager.getPhone(),
                manager.getRole(),
                branchMapper.toBranchResponse(manager.getBranch()),
                manager.getCreatedAt()
        );
    }
}
