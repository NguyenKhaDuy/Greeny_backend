package org.example.greenybackend.modules.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String paymentId,
        String transactionId,
        BigDecimal amount,
        String method,
        Integer status,
        String statusLabel,
        String gatewayResponse,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
