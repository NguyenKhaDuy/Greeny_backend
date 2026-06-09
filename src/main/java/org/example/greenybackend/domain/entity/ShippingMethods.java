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
@Table(name = "SHIPPING_METHODS")
public class ShippingMethods {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "SHIP_ID", nullable = false)
    private String shipId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "CODE")
    private String code;

    @Column(name = "BASE_FEE")
    private BigDecimal baseFee;

    @Column(name = "ESTIMATED_DAYS")
    private LocalDateTime estimatedDays;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "shippingMethods")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

}
