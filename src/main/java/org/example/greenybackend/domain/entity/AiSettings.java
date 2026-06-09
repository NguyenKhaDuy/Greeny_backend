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
@Table(name = "AI_SETTINGS")
public class AiSettings {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "AI_SETTINGS_ID", nullable = false)
    private String aiSettingsId;

    @Column(name = "PROVIDER")
    private String provider;

    @Column(name = "MODEL_NAME")
    private String modelName;

    @Column(name = "SYSTEM_PROMPT")
    private String systemPrompt;

    @Column(name = "TEMPERATURE")
    private BigDecimal temperature;

    @Column(name = "MAX_TOKENS")
    private Integer maxTokens;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT__")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
