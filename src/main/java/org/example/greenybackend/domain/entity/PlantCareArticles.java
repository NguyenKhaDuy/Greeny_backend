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
@Table(name = "PLANT_CARE_ARTICLES")
public class PlantCareArticles {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "PLANT_CARE_ARTICLES_ID", nullable = false)
    private String plantCareArticlesId;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "SLUG__")
    private String slug;

    @Column(name = "EXCERPT")
    private String excerpt;

    @Column(name = "CONTENT")
    private String content;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "THUMBNAIL_DATA", columnDefinition = "LONGBLOB")
    private byte[] thumbnailData;

    @Column(name = "THUMBNAIL_CONTENT_TYPE", length = 100)
    private String thumbnailContentType;

    @Column(name = "THUMBNAIL_FILE_NAME")
    private String thumbnailFileName;

    @Column(name = "THUMBNAIL_SIZE")
    private Long thumbnailSize;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @Column(name = "USER_ID", insertable = false, updatable = false)
    private String userId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "USER_ID", nullable = false)
    private UserEntity userEntity;

}
