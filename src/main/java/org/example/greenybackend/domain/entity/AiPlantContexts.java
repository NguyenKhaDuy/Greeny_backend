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
@Table(name = "AI_PLANT_CONTEXTS")
public class AiPlantContexts {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID_CONTEXT", nullable = false)
    private String idContext;

    @Column(name = "ISSUE_TYPE")
    private String issueType;

    @Column(name = "SYMPTOM_DESCRIPTION")
    private String symptomDescription;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "IMAGE_DATA", columnDefinition = "LONGBLOB")
    private byte[] imageData;

    @Column(name = "IMAGE_CONTENT_TYPE", length = 100)
    private String imageContentType;

    @Column(name = "IMAGE_FILE_NAME")
    private String imageFileName;

    @Column(name = "IMAGE_SIZE")
    private Long imageSize;

    @Column(name = "LIGHT_CONDITION")
    private String lightCondition;

    @Column(name = "WATERING_FREQUENCY")
    private String wateringFrequency;

    @Column(name = "SOIL_CONDITION")
    private String soilCondition;

    @Column(name = "HUMIDITY_LEVEL")
    private Integer humidityLevel;

    @Column(name = "TEMPERATURE")
    private BigDecimal temperature;

    @Column(name = "LOCATION_TYPE")
    private String locationType;

    @Column(name = "DIAGNOSIS_RESULT")
    private String diagnosisResult;

    @Column(name = "CARE_RECOMMENDATION")
    private String careRecommendation;

    @Column(name = "CONFIDENCE_SCORE")
    private Integer confidenceScore;

    @Column(name = "RECOMMENDED_PRODUCTS")
    private String recommendedProducts;

    @Column(name = "SELECTED_ITEMS")
    private String selectedItems;

    @Column(name = "ESTIMATED_TOTAL")
    private BigDecimal estimatedTotal;

    @Column(name = "PROMOTION_CODE")
    private String promotionCode;

    @Column(name = "EXPIRES_AT")
    private LocalDateTime expiresAt;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ID_SESSION", nullable = false, unique = true)
    private AiChat aiChat;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VARIANT_ID", nullable = false)
    private ProductVariant productVariant;

}
