package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;

public record RevenueSlowProductResponse(
        String variantId,
        String plantTitle,
        String variantName,
        String sku,
        Integer stockQuantity,
        Integer soldQuantity,
        BigDecimal revenue,
        String recommendation,
        String tone
) {
}
