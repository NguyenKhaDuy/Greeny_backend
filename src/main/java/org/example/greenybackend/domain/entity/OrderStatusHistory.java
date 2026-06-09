package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "ORDER_STATUS_HISTORY")
public class OrderStatusHistory {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ORDER_STATUS_HISTORY_ID", nullable = false)
    private String orderStatusHistoryId;

    @Column(name = "OLD_STATUS")
    private Integer oldStatus;

    @Column(name = "NEW_STATUS")
    private Integer newStatus;

    @Column(name = "NOTE")
    private String note;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Orders orders;

}
