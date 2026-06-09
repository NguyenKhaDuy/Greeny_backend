package org.example.greenybackend.modules.order.dto;

import java.time.LocalDateTime;

public record OrderHistoryResponse(
        String historyId,
        Integer oldStatus,
        Integer newStatus,
        String oldStatusLabel,
        String newStatusLabel,
        String note,
        LocalDateTime createdAt
) {
}
