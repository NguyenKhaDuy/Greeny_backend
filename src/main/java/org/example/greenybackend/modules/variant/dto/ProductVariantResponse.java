package org.example.greenybackend.modules.variant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductVariantResponse(
        String variantId,
        String plantId,
        String plantTitle,
        String name,
        String sku,
        Integer heightCm,
        Integer potSize,
        BigDecimal price,
        BigDecimal salePrice,
        Integer quantity,
        String attribute,
        Boolean isActive,
        String seoDescription,
        String seoTitle,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
