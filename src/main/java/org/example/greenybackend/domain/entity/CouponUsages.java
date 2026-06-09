package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "COUPON_USAGES")
public class CouponUsages {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID_COUPON_USAGES", nullable = false)
    private String idCouponUsages;

    @Column(name = "DISCOUNT_AMOUNT")
    private BigDecimal discountAmount;

    @Column(name = "USED_AT")
    private LocalDateTime usedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPONS_ID", nullable = false)
    private Coupons coupons;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
