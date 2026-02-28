package com.cargoapp.backend.branches.controller;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.dto.CreateBranchRequest;
import com.cargoapp.backend.branches.dto.UpdateBranchRequest;
import com.cargoapp.backend.branches.service.BranchService;
import com.cargoapp.backend.common.dto.PagedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/branches")
@RequiredArgsConstructor
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class BranchAdminController {

    private final BranchService branchService;

    @GetMapping
    public PagedResponse<BranchResponse> getAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return branchService.getAll(page, pageSize);
    }

    @GetMapping("/{id}")
    public BranchResponse getById(@PathVariable UUID id) {
        return branchService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BranchResponse create(@RequestBody @Valid CreateBranchRequest request) {
        return branchService.create(request);
    }

    @PatchMapping("/{id}")
    public BranchResponse update(@PathVariable UUID id, @RequestBody UpdateBranchRequest request) {
        return branchService.update(id, request);
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
