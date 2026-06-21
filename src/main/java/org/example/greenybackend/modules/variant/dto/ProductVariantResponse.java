package org.example.greenybackend.modules.variant.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        String imageUrl,
        List<String> images,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
