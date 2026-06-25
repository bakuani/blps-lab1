package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/additional-features")
@RequiredArgsConstructor
public class AdditionalFeatureController {

    private final AdditionalFeatureService additionalFeatureService;

    @GetMapping
    public List<AdditionalFeatureResponse> getFeatures() {
        return additionalFeatureService.getFeatures();
    }

    @GetMapping("/{id}")
    public AdditionalFeatureResponse getFeature(@PathVariable Long id) {
        return additionalFeatureService.getFeature(id);
    }

    @PostMapping
    public ResponseEntity<AdditionalFeatureResponse> createFeature(
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(additionalFeatureService.createFeature(request));
    }

    @PutMapping("/{id}")
    public AdditionalFeatureResponse updateFeature(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return additionalFeatureService.updateFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        additionalFeatureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }
}

