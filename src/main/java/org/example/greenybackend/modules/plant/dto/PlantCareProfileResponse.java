package org.example.greenybackend.modules.plant.dto;

public record PlantCareProfileResponse(
        String careId,
        String plantId,
        String plantTitle,
        String lightRequirement,
        String wateringFrequency,
        String humidityRequirement,
        Integer careLevel,
        String careInstruction
) {
}
