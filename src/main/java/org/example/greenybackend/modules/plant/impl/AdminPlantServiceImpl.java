package org.example.greenybackend.modules.plant.impl;

import static org.example.greenybackend.common.util.AdminFilters.contains;
import static org.example.greenybackend.common.util.AdminFilters.isBlankOrAll;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.example.greenybackend.domain.entity.Category;
import org.example.greenybackend.domain.entity.Plant;
import org.example.greenybackend.modules.category.CategoryRepository;
import org.example.greenybackend.modules.plant.AdminPlantService;
import org.example.greenybackend.modules.plant.PlantRepository;
import org.example.greenybackend.modules.plant.dto.PlantRequest;
import org.example.greenybackend.modules.plant.dto.PlantResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminPlantServiceImpl implements AdminPlantService {

    private final PlantRepository plantRepository;
    private final CategoryRepository categoryRepository;

    public AdminPlantServiceImpl(PlantRepository plantRepository, CategoryRepository categoryRepository) {
        this.plantRepository = plantRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    public List<PlantResponse> getAllPlants(Boolean includeDeleted) {
        return getAllPlants(includeDeleted, null, null, null, null, null, null, null, null);
    }

    @Override
    public List<PlantResponse> getAllPlants(
            Boolean includeDeleted,
            String title,
            String sku,
            String categoryId,
            String visibility,
            String variantState,
            String toxicity,
            Boolean petFriendly,
            Boolean airPurifying
    ) {
        List<Plant> plants = Boolean.TRUE.equals(includeDeleted)
                ? plantRepository.findAll()
                : plantRepository.findByDeletedAtIsNull();
        return plants.stream()
                .filter(plant -> contains(plant.getTitle(), title))
                .filter(plant -> contains(plant.getSku(), sku))
                .filter(plant -> matchesCategory(plant, categoryId))
                .filter(plant -> matchesVisibility(plant, visibility))
                .filter(plant -> matchesVariantState(plant, variantState))
                .filter(plant -> contains(plant.getToxicity(), toxicity))
                .filter(plant -> petFriendly == null || petFriendly.equals(plant.getPetFriendly()))
                .filter(plant -> airPurifying == null || airPurifying.equals(plant.getAirPurifying()))
                .map(this::toResponse)
                .toList();
    }

    @Override
    public PlantResponse getPlant(String plantId) {
        return toResponse(findPlant(plantId));
    }

    @Transactional
    @Override
    public PlantResponse createPlant(PlantRequest request) {
        validateTitle(request.title());

        LocalDateTime now = LocalDateTime.now();
        Plant plant = new Plant();
        plant.setPlantId(UUID.randomUUID().toString());
        applyRequest(plant, request);
        plant.setCreatedAt(now);
        plant.setUpdatedAt(now);
        return toResponse(plantRepository.save(plant));
    }

    @Transactional
    @Override
    public PlantResponse updatePlant(String plantId, PlantRequest request) {
        validateTitle(request.title());

        Plant plant = findPlant(plantId);
        applyRequest(plant, request);
        plant.setUpdatedAt(LocalDateTime.now());
        return toResponse(plant);
    }

    @Transactional
    @Override
    public void restorePlant(String plantId) {
        Plant plant = findPlant(plantId);
        plant.setDeletedAt(null);
        plant.setUpdatedAt(LocalDateTime.now());
    }

    @Transactional
    @Override
    public void softDeletePlant(String plantId) {
        Plant plant = findPlant(plantId);
        LocalDateTime now = LocalDateTime.now();
        plant.setDeletedAt(now);
        plant.setUpdatedAt(now);
    }

    private Plant findPlant(String plantId) {
        return plantRepository.findById(plantId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy cây"));
    }

    private boolean matchesCategory(Plant plant, String categoryId) {
        if (isBlankOrAll(categoryId)) {
            return true;
        }
        Category category = plant.getCategory();
        String normalizedCategoryId = categoryId.trim();
        if ("none".equalsIgnoreCase(normalizedCategoryId)) {
            return category == null || category.getCaId() == null;
        }
        return category != null && normalizedCategoryId.equals(category.getCaId());
    }

    private boolean matchesVisibility(Plant plant, String visibility) {
        if (isBlankOrAll(visibility)) {
            return true;
        }
        String normalizedVisibility = visibility.trim().toLowerCase();
        boolean visible = plant.getDeletedAt() == null;
        return switch (normalizedVisibility) {
            case "visible", "active", "true" -> visible;
            case "hidden", "deleted", "false" -> !visible;
            default -> true;
        };
    }

    private boolean matchesVariantState(Plant plant, String variantState) {
        if (isBlankOrAll(variantState)) {
            return true;
        }
        int variantCount = plant.getProductVariants() == null ? 0 : plant.getProductVariants().size();
        String normalizedState = variantState.trim().toLowerCase();
        return switch (normalizedState) {
            case "has", "with", "with-variants" -> variantCount > 0;
            case "none", "missing", "without", "without-variants" -> variantCount == 0;
            default -> true;
        };
    }

    private void applyRequest(Plant plant, PlantRequest request) {
        plant.setTitle(trimToNull(request.title()));
        plant.setSku(trimToNull(request.sku()));
        plant.setDescription(trimToNull(request.description()));
        plant.setScientificName(trimToNull(request.scientificName()));
        plant.setCommonName(trimToNull(request.commonName()));
        plant.setPlantType(request.plantType());
        plant.setOrigin(trimToNull(request.origin()));
        plant.setToxicity(trimToNull(request.toxicity()));
        plant.setPetFriendly(request.petFriendly());
        plant.setAirPurifying(request.airPurifying());
        plant.setCategory(findCategoryOrNull(request.categoryId()));
    }

    private Category findCategoryOrNull(String categoryId) {
        if (categoryId == null || categoryId.isBlank()) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy danh mục"));
    }

    private PlantResponse toResponse(Plant plant) {
        Category category = plant.getCategory();
        return new PlantResponse(
                plant.getPlantId(),
                plant.getTitle(),
                plant.getSku(),
                plant.getDescription(),
                plant.getScientificName(),
                plant.getCommonName(),
                plant.getPlantType(),
                plant.getOrigin(),
                plant.getToxicity(),
                plant.getPetFriendly(),
                plant.getAirPurifying(),
                category == null ? null : category.getCaId(),
                category == null ? null : category.getTitle(),
                plant.getCreatedAt(),
                plant.getUpdatedAt(),
                plant.getDeletedAt()
        );
    }

    private void validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Tên cây không được để trống");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
