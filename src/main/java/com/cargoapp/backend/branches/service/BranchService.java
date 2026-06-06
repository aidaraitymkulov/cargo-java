package com.cargoapp.backend.branches.service;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import com.cargoapp.backend.common.storage.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BranchService {

    private static final String SUBDIR = "branches";

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;
    private final ImageStorageService imageStorageService;

    @Transactional(readOnly = true)
    public List<BranchResponse> getAll() {
        return branchRepository.findAll()
                .stream()
                .map(branchMapper::toBranchResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BranchResponse getById(UUID id) {
        return branchMapper.toBranchResponse(getOrThrow(id));
    }

    @Transactional
    public BranchResponse create(CreateBranchRequest request, MultipartFile photo) {
        if (branchRepository.existsByPersonalCodePrefix(request.personalCodePrefix())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Префикс уже занят");
        }

        BranchEntity branch = new BranchEntity();
        branch.setAddress(request.address());
        branch.setPersonalCodePrefix(request.personalCodePrefix());
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        branch.setPhone(request.phone());
        branch.setWorkingHours(request.workingHours());
        if (photo != null && !photo.isEmpty()) {
            branch.setPhotoUrl(imageStorageService.save(photo, SUBDIR));
        }
        branchRepository.save(branch);

        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse update(UUID id, UpdateBranchRequest request, MultipartFile photo) {
        BranchEntity branch = getOrThrow(id);

        if (request.address() != null) branch.setAddress(request.address());
        if (request.latitude() != null) branch.setLatitude(request.latitude());
        if (request.longitude() != null) branch.setLongitude(request.longitude());
        if (request.phone() != null) branch.setPhone(request.phone());
        if (request.workingHours() != null) branch.setWorkingHours(request.workingHours());

        if (photo != null && !photo.isEmpty()) {
            final String oldPhoto = branch.getPhotoUrl();
            branch.setPhotoUrl(imageStorageService.save(photo, SUBDIR));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    imageStorageService.delete(oldPhoto, SUBDIR);
                }
            });
        }

        branchRepository.save(branch);
        return branchMapper.toBranchResponse(branch);
    }

    private BranchEntity getOrThrow(UUID id) {
        return branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));
    }
}