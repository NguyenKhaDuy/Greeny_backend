package org.example.greenybackend.modules.promotion;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.promotion.dto.CouponRequest;
import org.example.greenybackend.modules.promotion.dto.CouponResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponController {

    private final AdminCouponService couponService;

    public AdminCouponController(AdminCouponService couponService) {
        this.couponService = couponService;
    }

    @GetMapping
    public List<CouponResponse> getAll() {
        return couponService.getAllCoupons();
    }

    @GetMapping("/{couponId}")
    public CouponResponse getById(@PathVariable String couponId) {
        return couponService.getCoupon(couponId);
    }

    @PostMapping
    public ResponseEntity<CouponResponse> create(@RequestBody CouponRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(couponService.createCoupon(request));
    }

    @PutMapping("/{couponId}")
    public CouponResponse update(
            @PathVariable String couponId,
            @RequestBody CouponRequest request
    ) {
        return couponService.updateCoupon(couponId, request);
    }

    @DeleteMapping("/{couponId}")
    public MessageResponse deactivate(@PathVariable String couponId) {
        couponService.deactivateCoupon(couponId);
        return new MessageResponse("Da vo hieu hoa ma giam gia.");
    }
}
