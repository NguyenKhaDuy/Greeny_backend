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
@Table(name = "ORDERS")
public class Orders {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ORDER_ID", nullable = false)
    private String orderId;

    @Column(name = "SUBTOTAL")
    private BigDecimal subtotal;

    @Column(name = "DISCOUNT_AMOUNT")
    private BigDecimal discountAmount;

    @Column(name = "SHIPPING_FEE")
    private BigDecimal shippingFee;

    @Column(name = "TOTAL_PRICE")
    private BigDecimal totalPrice;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "PAYMENT_STATUS")
    private Integer paymentStatus;

    @Column(name = "NOTES")
    private String notes;

    @Column(name = "ESTIMATED_DELIVERY")
    private String estimatedDelivery;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "orders")
    @Builder.Default
    private List<OrderItems> orderItemsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "orders")
    @Builder.Default
    private List<ProductReviews> productReviewsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PAYMENTS_ID", nullable = false)
    private Payments payments;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ADDRESS_ID", nullable = false)
    private Address address;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "orders")
    @Builder.Default
    private List<ReturnRequests> returnRequestsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHIP_ID", nullable = false)
    private ShippingMethods shippingMethods;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SHIPMENTS_ID", nullable = false)
    private Shipments shipments;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "COUPONS_ID", nullable = false)
    private Coupons coupons;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "orders")
    @Builder.Default
    private List<OrderStatusHistory> orderStatusHistories = new ArrayList<>();

}
