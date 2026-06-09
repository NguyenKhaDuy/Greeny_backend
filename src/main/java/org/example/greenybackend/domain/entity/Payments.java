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
@Table(name = "PAYMENTS")
public class Payments {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "PAYMENTS_ID", nullable = false)
    private String paymentsId;

    @Column(name = "TRANSACTION_ID")
    private String transactionId;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Column(name = "METHOD")
    private String method;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "GATEWAY_RESPONSE")
    private String gatewayResponse;

    @Column(name = "PAID_AT")
    private LocalDateTime paidAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "payments")
    @Builder.Default
    private List<Orders> ordersList = new ArrayList<>();

}
