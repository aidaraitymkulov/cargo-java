package com.cargoapp.backend.branches.mapper;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.entity.BranchEntity;
import org.springframework.stereotype.Component;

@Component
public class BranchMapper {
    public BranchResponse toBranchResponse(BranchEntity branch) {
        return new BranchResponse(
                branch.getId(),
                branch.getAddress(),
                branch.getPersonalCodePrefix(),
                branch.isActive()
        );
    }
}
