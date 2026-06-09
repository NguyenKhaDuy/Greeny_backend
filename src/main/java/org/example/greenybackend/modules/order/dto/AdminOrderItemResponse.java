package org.example.greenybackend.modules.order.dto;

import java.math.BigDecimal;

public record AdminOrderItemResponse(
        String orderItemId,
        String variantId,
        String plantTitle,
        String variantName,
        String sku,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal totalPrice
) {
}
