package org.example.greenybackend.modules.plant;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.plant.dto.PlantRequest;
import org.example.greenybackend.modules.plant.dto.PlantResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/plants")
public class AdminPlantController {

    private final AdminPlantService plantService;

    public AdminPlantController(AdminPlantService plantService) {
        this.plantService = plantService;
    }

    @GetMapping
    public List<PlantResponse> getAll(
            @RequestParam(required = false, defaultValue = "false") Boolean includeDeleted,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String sku,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String variantState,
            @RequestParam(required = false) String toxicity,
            @RequestParam(required = false) Boolean petFriendly,
            @RequestParam(required = false) Boolean airPurifying
    ) {
        return plantService.getAllPlants(
                includeDeleted,
                title,
                sku,
                categoryId,
                visibility,
                variantState,
                toxicity,
                petFriendly,
                airPurifying
        );
    }

    @GetMapping("/{plantId}")
    public PlantResponse getById(@PathVariable String plantId) {
        return plantService.getPlant(plantId);
    }

    @PostMapping
    public ResponseEntity<PlantResponse> create(@RequestBody PlantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(plantService.createPlant(request));
    }

    @PutMapping("/{plantId}")
    public PlantResponse update(
            @PathVariable String plantId,
            @RequestBody PlantRequest request
    ) {
        return plantService.updatePlant(plantId, request);
    }

    @PatchMapping("/{plantId}/visibility")
    public MessageResponse setVisibility(
            @PathVariable String plantId,
            @RequestParam Boolean isVisible
    ) {
        if (Boolean.TRUE.equals(isVisible)) {
            plantService.restorePlant(plantId);
            return new MessageResponse("Da hien thi plant.");
        }
        plantService.softDeletePlant(plantId);
        return new MessageResponse("Da an plant.");
    }

    @DeleteMapping("/{plantId}")
    public MessageResponse softDelete(@PathVariable String plantId) {
        plantService.softDeletePlant(plantId);
        return new MessageResponse("Da xoa plant.");
    }
}
