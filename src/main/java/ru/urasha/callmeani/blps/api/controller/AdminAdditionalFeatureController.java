package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminAdditionalFeatureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/features")
@RequiredArgsConstructor
public class AdminAdditionalFeatureController {

    private final AdminAdditionalFeatureService adminAdditionalFeatureService;

    @GetMapping
    public List<AdditionalFeatureAdminResponse> getFeatures() {
        return adminAdditionalFeatureService.getFeatures();
    }

    @GetMapping("/{id}")
    public AdditionalFeatureAdminResponse getFeature(@PathVariable Long id) {
        return adminAdditionalFeatureService.getFeature(id);
    }

    @PostMapping
    public ResponseEntity<AdditionalFeatureAdminResponse> createFeature(
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAdditionalFeatureService.createFeature(request));
    }

    @PutMapping("/{id}")
    public AdditionalFeatureAdminResponse updateFeature(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return adminAdditionalFeatureService.updateFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        adminAdditionalFeatureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}


