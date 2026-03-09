package com.cargoapp.backend.products.controller.mobile;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.common.dto.PagedResponse;
import com.cargoapp.backend.products.dto.ProductHistoryResponse;
import com.cargoapp.backend.products.dto.ProductResponse;
import com.cargoapp.backend.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductMobileController {

    private final ProductService productService;

    @GetMapping("/my")
    public PagedResponse<ProductResponse> getMyProducts(
            @CurrentUserId UUID userId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        return productService.getMyProducts(userId, status, page, pageSize);
    }

    @GetMapping("/{productId}")
    public ProductResponse getProductById(
            @CurrentUserId UUID userId,
            @PathVariable UUID productId
    ) {
        return productService.getProductById(userId, productId);
    }

    @GetMapping("/{productId}/history")
    public ProductHistoryResponse getProductHistory(
            @CurrentUserId UUID userId,
            @PathVariable UUID productId
    ) {
        return productService.getProductHistory(userId, productId);
    }
}
