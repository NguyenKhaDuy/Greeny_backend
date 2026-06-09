package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RevenueRecentOrderResponse(
        String orderId,
        String customerName,
        LocalDateTime paidAt,
        String paymentMethod,
        BigDecimal totalPrice
) {
}
