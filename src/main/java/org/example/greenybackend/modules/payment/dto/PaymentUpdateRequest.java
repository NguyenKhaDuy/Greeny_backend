package org.example.greenybackend.modules.payment.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentUpdateRequest(
        String transactionId,
        BigDecimal amount,
        String method,
        Integer status,
        String gatewayResponse,
        LocalDateTime paidAt
) {
}
