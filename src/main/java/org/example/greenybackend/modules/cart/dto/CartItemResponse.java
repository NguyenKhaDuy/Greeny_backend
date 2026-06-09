package org.example.greenybackend.modules.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        String cartItemId,
        String variantId,
        String plantId,
        String plantTitle,
        String variantName,
        String sku,
        String imageUrl,
        BigDecimal unitPrice,
        BigDecimal originalPrice,
        Integer quantity,
        Integer stock,
        BigDecimal lineTotal,
        Boolean inStock
) {
}
