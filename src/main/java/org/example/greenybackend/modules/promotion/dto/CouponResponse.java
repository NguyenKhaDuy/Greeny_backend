package org.example.greenybackend.modules.promotion.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CouponResponse(
        String couponsId,
        String code,
        Integer type,
        BigDecimal value,
        BigDecimal minOrderAmount,
        BigDecimal maxDiscountAmount,
        Integer maxUses,
        Integer usedCount,
        Integer perUserLimit,
        Boolean isActive,
        LocalDateTime startsAt,
        LocalDateTime expiresAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
