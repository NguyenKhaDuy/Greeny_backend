package org.example.greenybackend.modules.promotion;

import org.example.greenybackend.domain.entity.CouponUsages;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponUsagesRepository extends JpaRepository<CouponUsages, String> {

    long countByCouponsCouponsIdAndUserEntityUserId(String couponId, String userId);
}
