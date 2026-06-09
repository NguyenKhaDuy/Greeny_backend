package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "COUPONS")
public class Coupons {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "COUPONS_ID", nullable = false)
    private String couponsId;

    @Column(name = "CODE")
    private String code;

    @Column(name = "TYPE")
    private Integer type;

    @Column(name = "VALUE")
    private BigDecimal value;

    @Column(name = "MIN_ORDER_AMOUNT")
    private BigDecimal minOrderAmount;

    @Column(name = "MAX_DISCOUNT_AMOUNT")
    private BigDecimal maxDiscountAmount;

    @Column(name = "MAX_USES")
    private Integer maxUses;

    @Column(name = "USED_COUNT")
    private Integer usedCount;

    @Column(name = "PER_USER_LIMIT")
    private Integer perUserLimit;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "STARTS_AT")
    private LocalDateTime startsAt;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "coupons")
    @Builder.Default
    private List<CouponUsages> couponUsagesList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "coupons")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

}
