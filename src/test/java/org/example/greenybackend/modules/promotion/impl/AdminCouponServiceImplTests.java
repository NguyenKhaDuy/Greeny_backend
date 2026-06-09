package org.example.greenybackend.modules.promotion.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.CouponUsages;
import org.example.greenybackend.domain.entity.Coupons;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.modules.order.OrdersRepository;
import org.example.greenybackend.modules.promotion.CouponUsagesRepository;
import org.example.greenybackend.modules.promotion.CouponsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminCouponServiceImplTests {

    @Mock
    private CouponsRepository couponsRepository;

    @Mock
    private OrdersRepository ordersRepository;

    @Mock
    private CouponUsagesRepository couponUsagesRepository;

    @InjectMocks
    private AdminCouponServiceImpl couponService;

    @Test
    void deleteCouponReassignsLinkedDataToNoCouponBeforeDeleting() {
        Coupons coupon = coupon("coupon-1", "GREENY10");
        Coupons noCoupon = coupon("coupon-none", "NO_COUPON");
        Orders order = new Orders();
        CouponUsages usage = new CouponUsages();
        order.setCoupons(coupon);
        usage.setCoupons(coupon);
        coupon.getOrdersList().add(order);
        coupon.getCouponUsagesList().add(usage);

        when(couponsRepository.findById("coupon-1")).thenReturn(Optional.of(coupon));
        when(couponsRepository.findAllByCodeIgnoreCase("NO_COUPON")).thenReturn(List.of(noCoupon));

        couponService.deleteCoupon("coupon-1");

        assertThat(order.getCoupons()).isSameAs(noCoupon);
        assertThat(usage.getCoupons()).isSameAs(noCoupon);
        verify(ordersRepository).saveAll(List.of(order));
        verify(couponUsagesRepository).saveAll(List.of(usage));
        verify(ordersRepository).flush();
        verify(couponUsagesRepository).flush();
        verify(couponsRepository).delete(coupon);
        verify(couponsRepository).flush();
    }

    @Test
    void deleteCouponRejectsSystemCoupon() {
        Coupons noCoupon = coupon("coupon-none", "NO_COUPON");
        when(couponsRepository.findById("coupon-none")).thenReturn(Optional.of(noCoupon));

        assertThatThrownBy(() -> couponService.deleteCoupon("coupon-none"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(couponsRepository).findById("coupon-none");
        verifyNoMoreInteractions(
                couponsRepository,
                ordersRepository,
                couponUsagesRepository
        );
    }

    private Coupons coupon(String id, String code) {
        Coupons coupon = new Coupons();
        coupon.setCouponsId(id);
        coupon.setCode(code);
        return coupon;
    }
}
