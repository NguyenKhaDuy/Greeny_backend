package org.example.greenybackend.modules.order;

import java.util.List;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.domain.entity.OrderStatusHistory;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;

public interface UserOrderService {

    int ORDER_PENDING = 0;
    int ORDER_CONFIRMED = 1;
    int ORDER_PREPARING = 2;
    int ORDER_SHIPPING = 3;
    int ORDER_DELIVERED = 4;
    int ORDER_CANCELED = 5;
    int ORDER_RETURNED = 6;

    int PAYMENT_PENDING = 0;
    int PAYMENT_PAID = 1;
    int PAYMENT_FAILED = 2;
    int PAYMENT_REFUNDED = 3;
    int PAYMENT_CANCELED = 4;

    List<UserOrderResponse> getOrders(UserEntity user);

    UserOrderResponse getOrder(UserEntity user, String orderId);

    UserOrderResponse cancelOrder(UserEntity user, String orderId);

    Orders findUserOrder(UserEntity user, String orderId);

    OrderStatusHistory addStatusHistory(Orders order, Integer oldStatus, Integer newStatus, String note);

    UserOrderResponse toResponse(Orders order);

    String orderStatusLabel(Integer status);

    String paymentStatusLabel(Integer status);

}
