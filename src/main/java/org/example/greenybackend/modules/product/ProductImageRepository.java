package org.example.greenybackend.modules.product;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.ProductImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductImageRepository extends JpaRepository<ProductImage, String> {

    List<ProductImage> findByProductVariantVariantIdOrderByCreatedAtAsc(String variantId);

    Optional<ProductImage> findFirstByProductVariantVariantIdOrderByCreatedAtAsc(String variantId);
}
