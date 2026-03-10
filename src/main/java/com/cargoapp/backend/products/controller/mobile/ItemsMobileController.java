package com.cargoapp.backend.products.controller.mobile;

import com.cargoapp.backend.common.annotation.CurrentUserId;
import com.cargoapp.backend.products.dto.ItemsSummaryResponse;
import com.cargoapp.backend.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemsMobileController {

    private final ProductService productService;

    @GetMapping("/summary")
    public ItemsSummaryResponse getSummary(@CurrentUserId UUID userId) {
        return productService.getItemsSummary(userId);
    }
}
