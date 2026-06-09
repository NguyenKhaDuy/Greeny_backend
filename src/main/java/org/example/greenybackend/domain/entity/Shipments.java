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
@Table(name = "SHIPMENTS")
public class Shipments {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "SHIPMENTS_ID", nullable = false)
    private String shipmentsId;

    @Column(name = "CARRIER_NAME")
    private String carrierName;

    @Column(name = "TRACKING_CODE")
    private String trackingCode;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "DELIVERED_AT")
    private LocalDateTime deliveredAt;

    @Column(name = "SHIPPING_FEE")
    private BigDecimal shippingFee;

    @Column(name = "SHIPPED_AT")
    private LocalDateTime shippedAt;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "shipments")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

}
