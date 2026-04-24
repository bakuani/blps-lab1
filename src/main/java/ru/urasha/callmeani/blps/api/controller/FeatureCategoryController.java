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
public class FeatureCategoryController {

    private final FeatureCategoryService featureCategoryService;

    @GetMapping
    public List<IdNameResponse> getFeatureCategories() {
        return featureCategoryService.getFeatureCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getFeatureCategory(@PathVariable Long id) {
        return featureCategoryService.getFeatureCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createFeatureCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(featureCategoryService.createFeatureCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateFeatureCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return featureCategoryService.updateFeatureCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureCategory(@PathVariable Long id) {
        featureCategoryService.deleteFeatureCategory(id);
        return ResponseEntity.noContent().build();
    }
}

