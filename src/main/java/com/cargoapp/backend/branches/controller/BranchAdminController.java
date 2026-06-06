package com.cargoapp.backend.branches.controller;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/branches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BranchAdminController {

    private final BranchService branchService;

    @GetMapping
    public List<BranchResponse> getAll() {
        return branchService.getAll();
    }

    @GetMapping("/{id}")
    public BranchResponse getById(@PathVariable UUID id) {
        return branchService.getById(id);
    }

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(
            @ModelAttribute @Valid CreateBranchRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return branchService.create(request, photo);
    }

    @PatchMapping(value = "/{id}", consumes = "multipart/form-data")
    public BranchResponse update(
            @PathVariable UUID id,
            @ModelAttribute UpdateBranchRequest request,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return branchService.update(id, request, photo);
    }

}