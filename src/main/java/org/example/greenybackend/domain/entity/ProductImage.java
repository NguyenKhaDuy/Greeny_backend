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
@Table(name = "PRODUCT_IMAGE")
public class ProductImage {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "IMAGE_ID", nullable = false)
    private String imageId;

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

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "VARIANT_ID", nullable = false)
    private ProductVariant productVariant;

}
