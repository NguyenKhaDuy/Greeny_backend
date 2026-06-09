package org.example.greenybackend.modules.order;

import org.example.greenybackend.domain.entity.UserEntity;
import org.example.greenybackend.modules.order.dto.CheckoutRequest;
import org.example.greenybackend.modules.order.dto.UserOrderResponse;
import org.example.greenybackend.modules.payment.dto.PaymentMockRequest;
import org.example.greenybackend.modules.promotion.dto.CouponApplyRequest;
import org.example.greenybackend.modules.promotion.dto.CouponPreviewResponse;

public interface UserCheckoutService {

    CouponPreviewResponse previewCoupon(UserEntity user, CouponApplyRequest request);

    UserOrderResponse checkout(UserEntity user, CheckoutRequest request);

    UserOrderResponse completeMockPayment(UserEntity user, String orderId, PaymentMockRequest request);

}
