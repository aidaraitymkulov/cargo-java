package com.cargoapp.backend.branches.service;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.entity.BranchEntity;
import com.cargoapp.backend.branches.mapper.BranchMapper;
import com.cargoapp.backend.branches.repository.BranchRepository;
import com.cargoapp.backend.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BranchService {

    private final BranchRepository branchRepository;
    private final BranchMapper branchMapper;

    @Value("${app.upload-dir}")
    private String uploadDir;

    public List<BranchResponse> getAll() {
        return branchRepository.findAll()
                .stream()
                .map(branchMapper::toBranchResponse)
                .toList();
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
    public BranchResponse create(CreateBranchRequest request, MultipartFile photo) {
        if (branchRepository.existsByPersonalCodePrefix(request.personalCodePrefix())) {
            throw new AppException("CONFLICT", HttpStatus.CONFLICT, "Префикс уже занят");
        }

        BranchEntity branch = new BranchEntity();
        branch.setAddress(request.address());
        branch.setPersonalCodePrefix(request.personalCodePrefix());
        branch.setLatitude(request.latitude());
        branch.setLongitude(request.longitude());
        if (photo != null && !photo.isEmpty()) {
            branch.setPhotoUrl(savePhoto(photo));
        }
        branchRepository.save(branch);

        return branchMapper.toBranchResponse(branch);
    }

    @Transactional
    public BranchResponse update(UUID id, UpdateBranchRequest request, MultipartFile photo) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException("BRANCH_NOT_FOUND", HttpStatus.NOT_FOUND, "Филиал не найден"));

        if (request.address() != null) branch.setAddress(request.address());
        if (request.latitude() != null) branch.setLatitude(request.latitude());
        if (request.longitude() != null) branch.setLongitude(request.longitude());

        String oldPhoto = null;
        if (photo != null && !photo.isEmpty()) {
            oldPhoto = branch.getPhotoUrl();
            branch.setPhotoUrl(savePhoto(photo));
        }

        branchRepository.save(branch);
        deletePhoto(oldPhoto);
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

    private String savePhoto(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Разрешены только изображения");
        }
        try {
            String original = file.getOriginalFilename();
            String safeName = (original != null)
                    ? Paths.get(original).getFileName().toString().replaceAll("[^a-zA-Z0-9._-]", "_")
                    : "upload";
            String filename = UUID.randomUUID() + "_" + safeName;
            Path path = Paths.get(uploadDir, "branches", filename);
            Files.createDirectories(path.getParent());
            file.transferTo(path);
            return "/uploads/branches/" + filename;
        } catch (IOException e) {
            throw new AppException("IMAGE_UPLOAD_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Ошибка при сохранении фото");
        }
    }

    private void deletePhoto(String photoUrl) {
        if (photoUrl == null) return;
        try {
            String filename = photoUrl.replace("/uploads/branches/", "");
            Path path = Paths.get(uploadDir, "branches", filename);
            Files.deleteIfExists(path);
        } catch (IOException e) {
            log.warn("Failed to delete branch photo '{}': {}", photoUrl, e.getMessage(), e);
        }
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
