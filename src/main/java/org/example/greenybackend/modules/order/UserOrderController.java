package org.example.greenybackend.modules.order;

import java.util.List;
import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user/orders")
public class UserOrderController {

    private final UserOrderService orderService;

    public UserOrderController(UserOrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<UserOrderResponse> orders(@AuthenticationPrincipal(expression = "user") UserEntity currentUser) {
        return orderService.getOrders(currentUser);
    }

    @GetMapping("/{orderId}")
    public UserOrderResponse order(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String orderId
    ) {
        return orderService.getOrder(currentUser, orderId);
    }

    @PostMapping("/{orderId}/cancel")
    public UserOrderResponse cancel(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String orderId
    ) {
        return orderService.cancelOrder(currentUser, orderId);
    }
}
