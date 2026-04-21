package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/features")
@RequiredArgsConstructor
public class AdditionalFeatureManagementController {

    private final AdditionalFeatureService AdditionalFeatureService;

    @GetMapping
    public List<AdditionalFeatureResponse> getFeatures() {
        return AdditionalFeatureService.getFeatures();
    }

    @GetMapping("/{id}")
    public AdditionalFeatureResponse getFeature(@PathVariable Long id) {
        return AdditionalFeatureService.getFeature(id);
    }

    @PostMapping
    public ResponseEntity<AdditionalFeatureResponse> createFeature(
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdditionalFeatureService.createFeature(request));
    }

    @PutMapping("/{id}")
    public AdditionalFeatureResponse updateFeature(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return AdditionalFeatureService.updateFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        AdditionalFeatureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}
