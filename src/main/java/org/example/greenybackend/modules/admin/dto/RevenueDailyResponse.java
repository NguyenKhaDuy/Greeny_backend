package org.example.greenybackend.modules.admin.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenueDailyResponse(
        LocalDate date,
        BigDecimal revenue,
        Integer paidOrders,
        Integer soldQuantity,
        Integer percentOfPeak
) {
}
