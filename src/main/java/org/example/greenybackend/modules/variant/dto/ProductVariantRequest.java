package org.example.greenybackend.modules.variant.dto;

import java.math.BigDecimal;

public record ProductVariantRequest(
        String plantId,
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
        String seoTitle
) {
}
