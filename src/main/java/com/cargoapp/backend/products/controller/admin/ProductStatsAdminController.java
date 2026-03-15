package com.cargoapp.backend.products.controller.admin;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.CountResponse;
import com.cargoapp.backend.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class ProductStatsAdminController {

    private final ProductService productService;

    @GetMapping("/stats")
    public CountResponse getProductStats(
            @CurrentUserId UUID currentUserId,
            @RequestParam(required = false) UUID branchId
    ) {
        return productService.getProductStats(currentUserId, branchId);
    }
}
