package com.cargoapp.backend.branches.service;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    public PagedResponse<BranchResponse> getAll(int page, int pageSize) {
        var pageable = PageRequest.of(page - 1, pageSize);
        var result = branchRepository.findAll(pageable);

        return new PagedResponse<>(
                result.getContent().stream().map(branchMapper::toBranchResponse).toList(),
                page,
                pageSize,
                result.getTotalElements()
        );
    }

    public List<BranchResponse> getAllActive() {
        return branchRepository.findAllByActiveTrue()
                .stream()
                .map(branchMapper::toBranchResponse)
                .toList();
    }

    public BranchResponse getById(UUID id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));
        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse create(CreateBranchRequest request) {
        if (branchRepository.existsByPersonalCodePrefix(request.personalCodePrefix())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Префикс уже занят");
        }

        BranchEntity branch = new BranchEntity();
        branch.setAddress(request.address());
        branch.setPersonalCodePrefix(request.personalCodePrefix());
        branchRepository.save(branch);

        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse update(UUID id, UpdateBranchRequest request) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));

        if (request.address() != null) {
            branch.setAddress(request.address());
        }

        branchRepository.save(branch);
        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse activate(UUID id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));
        branch.setActive(true);
        branchRepository.save(branch);
        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse deactivate(UUID id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));
        branch.setActive(false);
        branchRepository.save(branch);
        return branchMapper.toBranchResponse(branch);
    }
}
