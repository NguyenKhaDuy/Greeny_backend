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
@Table(name = "AI_CHAT")
public class AiChat {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID_SESSION", nullable = false)
    private String idSession;

    @Column(name = "CURRENT_STEP")
    private String currentStep;

    @Column(name = "STATUS")
    private Integer status;

    @Column(name = "STARTED_AT")
    private LocalDateTime startedAt;

    @Column(name = "ENDED_AT")
    private LocalDateTime endedAt;

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
    @OneToOne(mappedBy = "aiChat", fetch = FetchType.LAZY)
    private AiPlantContexts aiPlantContexts;

}
