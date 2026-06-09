package org.example.greenybackend.modules.order.dto;

import java.time.LocalDateTime;

public record AdminOrderHistoryResponse(
        String historyId,
        Integer oldStatus,
        String oldStatusLabel,
        Integer newStatus,
        String newStatusLabel,
        String note,
        LocalDateTime createdAt
) {
}
