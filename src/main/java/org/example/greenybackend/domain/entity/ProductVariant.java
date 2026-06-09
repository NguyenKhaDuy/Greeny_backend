package org.example.greenybackend.domain.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
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
@Table(name = "PRODUCT_VARIANT")
public class ProductVariant {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "VARIANT_ID", nullable = false)
    private String variantId;

    @Column(name = "NAME")
    private String name;

    @Column(name = "SKU")
    private String sku;

    @Column(name = "HEIGHT_CM")
    private Integer heightCm;

    @Column(name = "POT_SIZE")
    private Integer potSize;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "SALE_PRICE")
    private BigDecimal salePrice;

    @Column(name = "QUANTITY")
    private Integer quantity;

    @Column(name = "ATTRIBUTE")
    private String attribute;

    @Column(name = "IS_ACTIVE")
    private Boolean isActive;

    @Column(name = "SEO_DESCRIPTION")
    private String seoDescription;

    @Column(name = "SEO_TITLE")
    private String seoTitle;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productVariant")
    @Builder.Default
    private List<CartItem> cartItems = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productVariant")
    @Builder.Default
    private List<OrderItems> orderItemsList = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PLANT_ID", nullable = false)
    private Plant plant;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productVariant")
    @Builder.Default
    private List<ProductImage> productImages = new ArrayList<>();

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "productVariant")
    @Builder.Default
    private List<AiPlantContexts> aiPlantContextsList = new ArrayList<>();
}
