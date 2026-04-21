package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/feature-categories")
@RequiredArgsConstructor
public class FeatureCategoryManagementController {

    private final FeatureCategoryService FeatureCategoryService;

    @GetMapping
    public List<IdNameResponse> getFeatureCategories() {
        return FeatureCategoryService.getFeatureCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getFeatureCategory(@PathVariable Long id) {
        return FeatureCategoryService.getFeatureCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createFeatureCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(FeatureCategoryService.createFeatureCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateFeatureCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return FeatureCategoryService.updateFeatureCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureCategory(@PathVariable Long id) {
        FeatureCategoryService.deleteFeatureCategory(id);
        return ResponseEntity.noContent().build();
    }
}
