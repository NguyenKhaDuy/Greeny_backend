package org.example.greenybackend.modules.plant;

import java.util.List;
import org.example.greenybackend.modules.plant.dto.PlantCareProfileResponse;

public interface AdminCareService {

    List<PlantCareProfileResponse> getAllCareProfiles();

    List<PlantCareProfileResponse> getAllCareProfiles(
            String plant,
            Integer level,
            String light,
            String water,
            String humidity
    );

    PlantCareProfileResponse getCareProfile(String careId);

    PlantCareProfileResponse saveCareProfile(
            String plantId,
            String lightRequirement,
            String wateringFrequency,
            String humidityRequirement,
            Integer careLevel,
            String careInstruction
    );

    void deleteCareProfile(String careId);

}
