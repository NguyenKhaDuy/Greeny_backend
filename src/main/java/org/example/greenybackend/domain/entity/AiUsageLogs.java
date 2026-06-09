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
@Table(name = "AI_USAGE_LOGS")
public class AiUsageLogs {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "AI_USAGE_LOGS_ID", nullable = false)
    private String aiUsageLogsId;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "MODEL_NAME")
    private String modelName;

    @Column(name = "PROMPT_TOKENS")
    private Integer promptTokens;

    @Column(name = "COMPLETION_TOKENS__")
    private Integer completionTokens;

    @Column(name = "LATENCY_MS")
    private Integer latencyMs;

    @Column(name = "SUCCESS")
    private Boolean success;

    @Column(name = "ERROR_MESSAGE")
    private String errorMessage;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
