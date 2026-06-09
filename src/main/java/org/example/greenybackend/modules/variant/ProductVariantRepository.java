package org.example.greenybackend.modules.variant;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.ProductVariant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {

    List<ProductVariant> findByPlantPlantId(String plantId);

    @EntityGraph(attributePaths = {"plant", "plant.category", "productImages"})
    List<ProductVariant> findByIsActiveTrue();

    @EntityGraph(attributePaths = {"plant", "plant.category", "productImages"})
    Optional<ProductVariant> findByVariantIdAndIsActiveTrue(String variantId);
}
