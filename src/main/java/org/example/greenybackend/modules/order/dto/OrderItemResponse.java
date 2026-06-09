package org.example.greenybackend.modules.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        String orderItemId,
        String variantId,
        String plantId,
        String plantTitle,
        String variantName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice,
        String imageUrl
) {
}
