package org.example.greenybackend.modules.plant;

import java.util.List;
import org.example.greenybackend.common.response.MessageResponse;
import org.example.greenybackend.modules.plant.dto.PlantCareProfileResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/care-profiles")
public class AdminCareController {

    private final AdminCareService careService;

    public AdminCareController(AdminCareService careService) {
        this.careService = careService;
    }

    @GetMapping
    public List<PlantCareProfileResponse> getAll(
            @RequestParam(required = false) String plant,
            @RequestParam(required = false) Integer level,
            @RequestParam(required = false) String light,
            @RequestParam(required = false) String water,
            @RequestParam(required = false) String humidity
    ) {
        return careService.getAllCareProfiles(plant, level, light, water, humidity);
    }

    @GetMapping("/{careId}")
    public PlantCareProfileResponse getById(@PathVariable String careId) {
        return careService.getCareProfile(careId);
    }

    @PostMapping
    public ResponseEntity<PlantCareProfileResponse> createOrUpdate(
            @RequestParam String plantId,
            @RequestParam(required = false) String lightRequirement,
            @RequestParam(required = false) String wateringFrequency,
            @RequestParam(required = false) String humidityRequirement,
            @RequestParam(required = false) Integer careLevel,
            @RequestParam(required = false) String careInstruction
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(careService.saveCareProfile(
                plantId,
                lightRequirement,
                wateringFrequency,
                humidityRequirement,
                careLevel,
                careInstruction
        ));
    }

    @PutMapping("/{careId}")
    public PlantCareProfileResponse updateByCareId(
            @PathVariable String careId,
            @RequestParam String plantId,
            @RequestParam(required = false) String lightRequirement,
            @RequestParam(required = false) String wateringFrequency,
            @RequestParam(required = false) String humidityRequirement,
            @RequestParam(required = false) Integer careLevel,
            @RequestParam(required = false) String careInstruction
    ) {
        PlantCareProfileResponse existing = careService.getCareProfile(careId);
        if (plantId == null || !plantId.equals(existing.plantId())) {
            throw new IllegalArgumentException("Khong the doi cay cua ho so cham soc qua endpoint nay");
        }
        return careService.saveCareProfile(
                plantId,
                lightRequirement,
                wateringFrequency,
                humidityRequirement,
                careLevel,
                careInstruction
        );
    }

    @DeleteMapping("/{careId}")
    public MessageResponse delete(@PathVariable String careId) {
        careService.deleteCareProfile(careId);
        return new MessageResponse("Da xoa ho so cham soc cay.");
    }
}
