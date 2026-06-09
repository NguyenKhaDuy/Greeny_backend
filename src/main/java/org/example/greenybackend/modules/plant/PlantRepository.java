package org.example.greenybackend.modules.plant;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.Plant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantRepository extends JpaRepository<Plant, String> {

    List<Plant> findByDeletedAtIsNull();

    long countByCategory_CaId(String categoryId);

    @EntityGraph(attributePaths = {"category", "productVariants"})
    Optional<Plant> findByPlantIdAndDeletedAtIsNull(String plantId);
}
