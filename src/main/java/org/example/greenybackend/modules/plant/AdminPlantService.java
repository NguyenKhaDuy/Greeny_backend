package org.example.greenybackend.modules.plant;

import java.util.List;
import org.example.greenybackend.modules.plant.dto.PlantRequest;
import org.example.greenybackend.modules.plant.dto.PlantResponse;

public interface AdminPlantService {

    List<PlantResponse> getAllPlants(Boolean includeDeleted);

    List<PlantResponse> getAllPlants(
            Boolean includeDeleted,
            String title,
            String sku,
            String categoryId,
            String visibility,
            String variantState,
            String toxicity,
            Boolean petFriendly,
            Boolean airPurifying
    );

    PlantResponse getPlant(String plantId);

    PlantResponse createPlant(PlantRequest request);

    PlantResponse updatePlant(String plantId, PlantRequest request);

    void restorePlant(String plantId);

    void softDeletePlant(String plantId);

}
