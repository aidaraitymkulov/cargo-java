package com.cargoapp.backend.products.controller.admin;

import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.products.dto.ProductAdminResponse;
import com.cargoapp.backend.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users/{userId}/products")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('MANAGER', 'SUPER_ADMIN')")
public class ProductAdminController {

    private final ProductService productService;

    @GetMapping
    public PagedResponse<ProductAdminResponse> getUserProducts(
            @PathVariable UUID userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        return productService.getUserProductsForAdmin(userId, page, pageSize);
    }
}
