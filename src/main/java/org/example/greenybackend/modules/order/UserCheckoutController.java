package org.example.greenybackend.modules.order;

import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.order.dto.CheckoutRequest;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;
import org.example.greenybackend.modules.payment.dto.PaymentMockRequest;
import org.example.greenybackend.modules.promotion.dto.CouponApplyRequest;
import org.example.greenybackend.modules.promotion.dto.CouponPreviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserCheckoutController {

    private final UserCheckoutService checkoutService;

    public UserCheckoutController(UserCheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    @PostMapping("/coupons/validate")
    public CouponPreviewResponse validateCoupon(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody CouponApplyRequest request
    ) {
        return checkoutService.previewCoupon(currentUser, request);
    }

    @PostMapping("/orders/checkout")
    public ResponseEntity<UserOrderResponse> checkout(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @RequestBody CheckoutRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(checkoutService.checkout(currentUser, request));
    }

    @PostMapping("/orders/{orderId}/payment/mock")
    public UserOrderResponse completeMockPayment(
            @AuthenticationPrincipal(expression = "user") UserEntity currentUser,
            @PathVariable String orderId,
            @RequestBody PaymentMockRequest request
    ) {
        return checkoutService.completeMockPayment(currentUser, orderId, request);
    }
}
