package org.example.greenybackend.modules.promotion.dto;

import java.math.BigDecimal;

public record CouponApplyRequest(
        String couponCode,
        BigDecimal subtotal
) {
}
