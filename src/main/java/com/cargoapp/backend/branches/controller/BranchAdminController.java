package com.cargoapp.backend.branches.controller;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.service.BranchService;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Validated
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
            @RequestParam @NotBlank String address,
            @RequestParam @NotBlank String personalCodePrefix,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return branchService.create(new CreateBranchRequest(address, personalCodePrefix, latitude, longitude), photo);
    }

    @PatchMapping(value = "/{id}", consumes = "multipart/form-data")
    public BranchResponse update(
            @PathVariable UUID id,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestPart(value = "photo", required = false) MultipartFile photo
    ) {
        return branchService.update(id, new UpdateBranchRequest(address, latitude, longitude), photo);
    }

    @PatchMapping("/{id}/activate")
    public BranchResponse activate(@PathVariable UUID id) {
        return branchService.activate(id);
    }

    @PatchMapping("/{id}/deactivate")
    public BranchResponse deactivate(@PathVariable UUID id) {
        return branchService.deactivate(id);
    }
}
