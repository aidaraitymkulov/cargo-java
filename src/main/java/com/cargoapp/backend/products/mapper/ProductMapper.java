package com.cargoapp.backend.products.mapper;

import com.cargoapp.backend.products.dto.ProductAdminResponse;
import com.cargoapp.backend.products.dto.ProductHistoryEntry;
import com.cargoapp.backend.products.dto.ProductResponse;
import com.cargoapp.backend.products.entity.ProductEntity;
import com.cargoapp.backend.products.entity.ProductHistoryEntity;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(ProductEntity product) {
        return new ProductResponse(
                product.getId(),
                product.getHatch(),
                product.getStatus(),
                product.getOrderId(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductAdminResponse toProductAdminResponse(ProductEntity product) {
        return new ProductAdminResponse(
                product.getId(),
                product.getHatch(),
                product.getUser().getId(),
                product.getUser().getFirstName(),
                product.getUser().getLastName(),
                product.getUser().getPersonalCode(),
                product.getOrderId(),
                product.getStatus(),
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    public ProductHistoryEntry toHistoryEntry(ProductHistoryEntity history) {
        return new ProductHistoryEntry(
                history.getId(),
                history.getStatus(),
                history.getCreatedAt()
        );
    }
}
