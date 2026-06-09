package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Entity
@Table(name = "PLANT_CARE_PROFILE")
public class PlantCareProfile {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "CARE_ID", nullable = false)
    private String careId;

    @Column(name = "LIGHT_REQUIREMENT")
    private String lightRequirement;

    @Column(name = "WATERING_FREQUENCY")
    private String wateringFrequency;

    @Column(name = "HUMIDITY_REQUIREMENT")
    private String humidityRequirement;

    @Column(name = "CARE_LEVEL")
    private Integer careLevel;

    @Column(name = "CARE_INSTRUCTION")
    private String careInstruction;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLANT_ID", nullable = false)
    private Plant plant;

}
