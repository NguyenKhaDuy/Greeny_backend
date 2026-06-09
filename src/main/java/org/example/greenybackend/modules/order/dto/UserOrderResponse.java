package org.example.greenybackend.modules.order.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.example.greenybackend.modules.payment.dto.PaymentResponse;

public record UserOrderResponse(
        String orderId,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        String shippingMethod,
        String carrierName,
        String trackingCode,
        Integer shipmentStatus,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal shippingFee,
        BigDecimal totalPrice,
        Integer status,
        String statusLabel,
        Integer paymentStatus,
        String paymentStatusLabel,
        String notes,
        String estimatedDelivery,
        String couponCode,
        PaymentResponse payment,
        List<OrderItemResponse> items,
        List<OrderHistoryResponse> histories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Boolean canCancel,
        Boolean canReview
) {
}
