package org.example.greenybackend.modules.promotion;

import java.util.List;
import org.example.greenybackend.modules.promotion.dto.CouponRequest;
import org.example.greenybackend.modules.promotion.dto.CouponResponse;

public interface AdminCouponService {

    List<CouponResponse> getAllCoupons();

    CouponResponse getCoupon(String couponId);

    CouponResponse createCoupon(CouponRequest request);

    CouponResponse updateCoupon(String couponId, CouponRequest request);

    void deactivateCoupon(String couponId);

    void deleteCoupon(String couponId);

}
