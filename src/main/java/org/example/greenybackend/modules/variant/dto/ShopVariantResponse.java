package org.example.greenybackend.modules.variant.dto;

import java.math.BigDecimal;
import java.util.List;

public record ShopVariantResponse(
        String variantId,
        String plantId,
        String plantTitle,
        String name,
        String sku,
        Integer heightCm,
        Integer potSize,
        BigDecimal price,
        BigDecimal salePrice,
        BigDecimal effectivePrice,
        Integer quantity,
        String attribute,
        Boolean isActive,
        String imageUrl,
        List<String> images
) {
}
