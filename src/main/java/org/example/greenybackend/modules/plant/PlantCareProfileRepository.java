package org.example.greenybackend.modules.plant;

import java.util.List;
import java.util.Optional;
import org.example.greenybackend.domain.entity.PlantCareProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlantCareProfileRepository extends JpaRepository<PlantCareProfile, String> {

    List<PlantCareProfile> findByPlantPlantId(String plantId);

    Optional<PlantCareProfile> findFirstByPlantPlantId(String plantId);
}
