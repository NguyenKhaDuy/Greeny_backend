package org.example.greenybackend.modules.promotion.dto;

import java.math.BigDecimal;

public record CouponPreviewResponse(
        String couponId,
        String code,
        Integer type,
        BigDecimal value,
        BigDecimal discountAmount,
        String message,
        Boolean valid
) {
}
