package org.example.greenybackend.modules.order.dto;

public record OrderStatusUpdateRequest(
        Integer status,
        Integer paymentStatus,
        String note,
        String estimatedDelivery
) {
}
