package org.example.greenybackend.modules.plant.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.domain.entity.PlantCareProfile;
import org.example.greenybackend.modules.plant.AdminCareService;
import org.example.greenybackend.modules.plant.PlantCareProfileRepository;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.plant.dto.PlantCareProfileResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminCareServiceImpl implements AdminCareService {

    private final PlantCareProfileRepository careProfileRepository;
    private final PlantRepository plantRepository;

    public AdminCareServiceImpl(PlantCareProfileRepository careProfileRepository, PlantRepository plantRepository) {
        this.careProfileRepository = careProfileRepository;
        this.plantRepository = plantRepository;
    }

    @Override
    public List<PlantCareProfileResponse> getAllCareProfiles() {
        return getAllCareProfiles(null, null, null, null, null);
    }

    @Override
    public List<PlantCareProfileResponse> getAllCareProfiles(
            String plant,
            Integer level,
            String light,
            String water,
            String humidity
    ) {
        return careProfileRepository.findAll().stream()
                .sorted(Comparator.comparing(this::plantTitle, Comparator.nullsLast(String::compareToIgnoreCase)))
                .filter(profile -> contains(profile.getPlant() == null ? null : profile.getPlant().getTitle(), plant))
                .filter(profile -> level == null || level.equals(profile.getCareLevel()))
                .filter(profile -> contains(profile.getLightRequirement(), light))
                .filter(profile -> contains(profile.getWateringFrequency(), water))
                .filter(profile -> contains(profile.getHumidityRequirement(), humidity))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PlantCareProfileResponse getCareProfile(String careId) {
        return toResponse(findCareProfile(careId));
    }

    @Transactional
    @Override
    public PlantCareProfileResponse saveCareProfile(
            String plantId,
            String lightRequirement,
            String wateringFrequency,
            String humidityRequirement,
            Integer careLevel,
            String careInstruction
    ) {
        Plant plant = plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay cay"));
        PlantCareProfile profile = careProfileRepository.findFirstByPlantPlantId(plantId)
                .orElseGet(() -> {
                    PlantCareProfile created = new PlantCareProfile();
                    created.setCareId(UUID.randomUUID().toString());
                    created.setPlant(plant);
                    return created;
                });

        profile.setLightRequirement(trimToNull(lightRequirement));
        profile.setWateringFrequency(trimToNull(wateringFrequency));
        profile.setHumidityRequirement(trimToNull(humidityRequirement));
        profile.setCareLevel(careLevel);
        profile.setCareInstruction(trimToNull(careInstruction));
        return toResponse(careProfileRepository.save(profile));
    }

    @Transactional
    @Override
    public void deleteCareProfile(String careId) {
        careProfileRepository.delete(findCareProfile(careId));
    }

    private PlantCareProfile findCareProfile(String careId) {
        return careProfileRepository.findById(careId)
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay ho so cham soc cay"));
    }

    private String plantTitle(PlantCareProfile profile) {
        Plant plant = profile.getPlant();
        return plant == null ? null : plant.getTitle();
    }

    private PlantCareProfileResponse toResponse(PlantCareProfile profile) {
        Plant plant = profile.getPlant();
        return new PlantCareProfileResponse(
                profile.getCareId(),
                plant == null ? null : plant.getPlantId(),
                plant == null ? null : plant.getTitle(),
                profile.getLightRequirement(),
                profile.getWateringFrequency(),
                profile.getHumidityRequirement(),
                profile.getCareLevel(),
                profile.getCareInstruction()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
