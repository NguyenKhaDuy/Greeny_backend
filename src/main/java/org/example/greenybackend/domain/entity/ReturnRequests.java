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
@Table(name = "RETURN_REQUESTS")
public class ReturnRequests {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "RETURN_REQUESTS_ID", nullable = false)
    private String returnRequestsId;

    @Column(name = "REASON")
    private String reason;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "EVIDENCE_IMAGES")
    private String evidenceImages;

    @Column(name = "ADMIN_NOTE")
    private String adminNote;

    @Column(name = "HANDLED_AT")
    private LocalDateTime handledAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Orders orders;

}
