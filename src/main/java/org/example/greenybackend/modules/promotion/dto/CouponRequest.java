package org.example.greenybackend.modules.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponRequest(
        String code,
        Integer type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUses,
        Integer perUserLimit,
        Boolean isActive,
        LocalDateTime startsAt,
        LocalDateTime expiresAt
) {
}
