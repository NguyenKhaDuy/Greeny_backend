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
@Table(name = "PRODUCT_REVIEWS")
public class ProductReviews {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "PRODUCT_REVIEWS_ID", nullable = false)
    private String productReviewsId;

    @Column(name = "RATING")
    private Integer rating;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "COMMENT")
    private String comment;

    @Column(name = "IMAGES")
    private String images;

    @Column(name = "IS_APPROVED")
    private Boolean isApproved;

    @Column(name = "HELPFUL_COUNT")
    private Integer helpfulCount;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORDER_ID", nullable = false)
    private Orders orders;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLANT_ID", nullable = false)
    private Plant plant;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
