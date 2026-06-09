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
@Table(name = "CATEGORY")
public class Category {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "CA_ID", nullable = false)
    private String caId;

    @Column(name = "TITLE", length = 50)
    private String title;

    @Column(name = "DESCRIPTION")
    private String description;

    @Column(name = "IMAGE_URL")
    private String imageUrl;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "SORT_ORDER")
    private Integer sortOrder;

    @Column(name = "CREATED_CA")
    private LocalDateTime createdCa;

    @Column(name = "UPDATED_CA")
    private LocalDateTime updatedCa;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "category")
    @Builder.Default
    private List<Plant> plants = new ArrayList<>();

}
