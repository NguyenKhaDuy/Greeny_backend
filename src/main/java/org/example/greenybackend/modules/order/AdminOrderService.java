package org.example.greenybackend.modules.order;

import java.util.List;
import org.example.greenybackend.modules.order.dto.AdminOrderResponse;
import org.example.greenybackend.modules.order.dto.OrderStatusUpdateRequest;
import org.example.greenybackend.modules.payment.dto.PaymentUpdateRequest;

public interface AdminOrderService {

    List<AdminOrderResponse> getAllOrders(Integer status, Integer paymentStatus);

    AdminOrderResponse getOrder(String orderId);

    AdminOrderResponse updateOrderStatus(String orderId, OrderStatusUpdateRequest request);

    AdminOrderResponse updatePayment(String orderId, PaymentUpdateRequest request);

    String orderStatusLabel(Integer status);

    String paymentStatusLabel(Integer status);

}
