package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.service.AdminFeatureCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/feature-categories")
@RequiredArgsConstructor
public class AdminFeatureCategoryController {

    private final AdminFeatureCategoryService adminFeatureCategoryService;

    @GetMapping
    public List<IdNameResponse> getFeatureCategories() {
        return adminFeatureCategoryService.getFeatureCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getFeatureCategory(@PathVariable Long id) {
        return adminFeatureCategoryService.getFeatureCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createFeatureCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminFeatureCategoryService.createFeatureCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateFeatureCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminFeatureCategoryService.updateFeatureCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeatureCategory(@PathVariable Long id) {
        adminFeatureCategoryService.deleteFeatureCategory(id);
        return ResponseEntity.noContent().build();
    }
}

