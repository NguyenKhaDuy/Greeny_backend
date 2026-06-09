package org.example.greenybackend.modules.order;

import java.util.List;
import org.example.greenybackend.modules.order.dto.AdminOrderResponse;
import org.example.greenybackend.modules.order.dto.OrderStatusUpdateRequest;
import org.example.greenybackend.modules.payment.dto.PaymentUpdateRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderController {

    private final AdminOrderService orderService;

    public AdminOrderController(AdminOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<AdminOrderResponse> getAll(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer paymentStatus
    ) {
        return orderService.getAllOrders(status, paymentStatus);
    }

    @GetMapping("/{orderId}")
    public AdminOrderResponse getById(@PathVariable String orderId) {
        return orderService.getOrder(orderId);
    }

    @PutMapping("/{orderId}/status")
    public AdminOrderResponse updateStatus(
            @PathVariable String orderId,
            @RequestBody OrderStatusUpdateRequest request
    ) {
        return orderService.updateOrderStatus(orderId, request);
    }

    @PutMapping("/{orderId}/payment")
    public AdminOrderResponse updatePayment(
            @PathVariable String orderId,
            @RequestBody PaymentUpdateRequest request
    ) {
        return orderService.updatePayment(orderId, request);
    }
}
