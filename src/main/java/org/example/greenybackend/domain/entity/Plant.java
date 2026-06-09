package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
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
@Table(name = "PLANT")
public class Plant {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "PLANT_ID", nullable = false)
    private String plantId;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "SKU")
    private String sku;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "SCIENTIFIC_NAME")
    private String scientificName;

    @Column(name = "COMMON_NAME")
    private String commonName;

    @Column(name = "PLANT_TYPE")
    private Integer plantType;

    @Column(name = "ORIGIN")
    private String origin;

    @Column(name = "TOXICITY")
    private String toxicity;

    @Column(name = "PET_FRIENDLY")
    private Boolean petFriendly;

    @Column(name = "AIR_PURIFYING")
    private Boolean airPurifying;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "DELETED_AT")
    private LocalDateTime deletedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "CA_ID", nullable = true)
    private Category category;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "plant")
    @Builder.Default
    private List<PlantCareProfile> plantCareProfiles = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "plant")
    @Builder.Default
    private List<Favorite> favorites = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "plant")
    @Builder.Default
    private List<ProductReviews> productReviewsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "plant")
    @Builder.Default
    private List<ProductVariant> productVariants = new ArrayList<>();

}
