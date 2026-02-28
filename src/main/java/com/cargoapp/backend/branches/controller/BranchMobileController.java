package com.cargoapp.backend.branches.controller;

import com.cargoapp.backend.branches.dto.BranchResponse;
import com.cargoapp.backend.branches.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/branches")
@RequiredArgsConstructor
public class BranchMobileController {

    private final BranchService branchService;

    @GetMapping
    public List<BranchResponse> getAllActive() {
        return branchService.getAllActive();
    }
}
