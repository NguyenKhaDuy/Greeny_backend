package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;

public record RevenueBreakdownResponse(
        String key,
        String label,
        Integer count,
        BigDecimal revenue,
        Integer percent,
        String tone
) {
}
