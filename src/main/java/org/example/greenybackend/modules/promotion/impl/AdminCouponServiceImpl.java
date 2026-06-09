package org.example.greenybackend.modules.promotion.impl;

import org.example.greenybackend.modules.promotion.AdminCouponService;
import org.example.greenybackend.modules.order.OrdersRepository;
import org.example.greenybackend.modules.promotion.CouponUsagesRepository;
import org.example.greenybackend.modules.promotion.CouponsRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.example.greenybackend.domain.entity.CouponUsages;
import org.example.greenybackend.domain.entity.Coupons;
import org.example.greenybackend.domain.entity.Orders;
import org.example.greenybackend.modules.promotion.dto.CouponRequest;
import org.example.greenybackend.modules.promotion.dto.CouponResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCouponServiceImpl implements AdminCouponService {

    private final CouponsRepository couponsRepository;
    private final OrdersRepository ordersRepository;
    private final CouponUsagesRepository couponUsagesRepository;

    public AdminCouponServiceImpl(
            CouponsRepository couponsRepository,
            OrdersRepository ordersRepository,
            CouponUsagesRepository couponUsagesRepository
    ) {
        this.couponsRepository = couponsRepository;
        this.ordersRepository = ordersRepository;
        this.couponUsagesRepository = couponUsagesRepository;
    }

    @Override
    public List<CouponResponse> getAllCoupons() {
        return couponsRepository.findAll().stream()
                .sorted(Comparator.comparing(Coupons::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public CouponResponse getCoupon(String couponId) {
        return toResponse(findCoupon(couponId));
    }

    @Transactional
    @Override
    public CouponResponse createCoupon(CouponRequest request) {
        validateRequest(request, null);

        LocalDateTime now = LocalDateTime.now();
        Coupons coupon = new Coupons();
        coupon.setCouponsId(UUID.randomUUID().toString());
        applyRequest(coupon, request);
        coupon.setUsedCount(0);
        coupon.setIsActive(request.isActive() == null || request.isActive());
        coupon.setCreatedAt(now);
        coupon.setUpdatedAt(now);
        return toResponse(couponsRepository.save(coupon));
    }

    @Transactional
    @Override
    public CouponResponse updateCoupon(String couponId, CouponRequest request) {
        validateRequest(request, couponId);

        Coupons coupon = findCoupon(couponId);
        applyRequest(coupon, request);
        coupon.setUpdatedAt(LocalDateTime.now());
        return toResponse(coupon);
    }

    @Transactional
    @Override
    public void deactivateCoupon(String couponId) {
        Coupons coupon = findCoupon(couponId);
        coupon.setIsActive(false);
        coupon.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void deleteCoupon(String couponId) {
        Coupons coupon = findCoupon(couponId);
        if ("NO_COUPON".equals(coupon.getCode())) {
            throw new IllegalArgumentException("Không thể xóa mã hệ thống");
        }
        List<Orders> linkedOrders = new ArrayList<>(coupon.getOrdersList());
        List<CouponUsages> linkedUsages = new ArrayList<>(coupon.getCouponUsagesList());
        if (!linkedOrders.isEmpty() || !linkedUsages.isEmpty()) {
            Coupons fallbackCoupon = findOrCreateNoCoupon(coupon.getCouponsId());
            linkedOrders.forEach(order -> order.setCoupons(fallbackCoupon));
            linkedUsages.forEach(usage -> usage.setCoupons(fallbackCoupon));
            ordersRepository.saveAll(linkedOrders);
            couponUsagesRepository.saveAll(linkedUsages);
            ordersRepository.flush();
            couponUsagesRepository.flush();
        }
        couponsRepository.delete(coupon);
        couponsRepository.flush();
    }

    private Coupons findCoupon(String couponId) {
        return couponsRepository.findById(couponId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy mã giảm giá"));
    }

    private Coupons findOrCreateNoCoupon(String excludedCouponId) {
        return couponsRepository.findAllByCodeIgnoreCase("NO_COUPON").stream()
                .filter(coupon -> !coupon.getCouponsId().equals(excludedCouponId))
                .findFirst()
                .orElseGet(this::createNoCoupon);
    }

    private Coupons createNoCoupon() {
        LocalDateTime now = LocalDateTime.now();
        Coupons coupon = new Coupons();
        coupon.setCouponsId(UUID.randomUUID().toString());
        coupon.setCode("NO_COUPON");
        coupon.setType(2);
        coupon.setValue(BigDecimal.ZERO);
        coupon.setMinOrderAmount(BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(BigDecimal.ZERO);
        coupon.setMaxUses(null);
        coupon.setUsedCount(0);
        coupon.setPerUserLimit(null);
        coupon.setIsActive(true);
        coupon.setStartsAt(null);
        coupon.setExpiresAt(null);
        coupon.setCreatedAt(now);
        coupon.setUpdatedAt(now);
        return couponsRepository.saveAndFlush(coupon);
    }

    private void applyRequest(Coupons coupon, CouponRequest request) {
        coupon.setCode(normalizeCode(request.code()));
        coupon.setType(request.type());
        coupon.setValue(request.value());
        coupon.setMinOrderAmount(defaultMoney(request.minOrderAmount()));
        coupon.setMaxDiscountAmount(request.maxDiscountAmount());
        coupon.setMaxUses(request.maxUses());
        coupon.setPerUserLimit(request.perUserLimit());
        if (request.isActive() != null) {
            coupon.setIsActive(request.isActive());
        }
        coupon.setStartsAt(request.startsAt());
        coupon.setExpiresAt(request.expiresAt());
    }

    private CouponResponse toResponse(Coupons coupon) {
        return new CouponResponse(
                coupon.getCouponsId(),
                coupon.getCode(),
                coupon.getType(),
                coupon.getValue(),
                coupon.getMinOrderAmount(),
                coupon.getMaxDiscountAmount(),
                coupon.getMaxUses(),
                coupon.getUsedCount(),
                coupon.getPerUserLimit(),
                coupon.getIsActive(),
                coupon.getStartsAt(),
                coupon.getExpiresAt(),
                coupon.getCreatedAt(),
                coupon.getUpdatedAt()
        );
    }

    private void validateRequest(CouponRequest request, String currentCouponId) {
        String code = normalizeCode(request.code());
        if (code == null) {
            throw new IllegalArgumentException("Mã giảm giá không được để trống");
        }
        couponsRepository.findByCodeIgnoreCase(code)
                .filter(existing -> !existing.getCouponsId().equals(currentCouponId))
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Mã giảm giá đã tồn tại");
                });
        if (request.type() == null) {
            throw new IllegalArgumentException("Loại giảm giá không được để trống");
        }
        if (request.value() == null || request.value().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá trị giảm giá phải lớn hơn 0");
        }
        if (request.maxUses() != null && request.maxUses() < 0) {
            throw new IllegalArgumentException("Số lượt sử dụng tối đa không hợp lệ");
        }
        if (request.perUserLimit() != null && request.perUserLimit() < 0) {
            throw new IllegalArgumentException("Giới hạn mỗi khách không hợp lệ");
        }
        if (request.expiresAt() != null && request.startsAt() != null && request.expiresAt().isBefore(request.startsAt())) {
            throw new IllegalArgumentException("Ngày hết hạn phải sau ngày bắt đầu");
        }
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }
}
