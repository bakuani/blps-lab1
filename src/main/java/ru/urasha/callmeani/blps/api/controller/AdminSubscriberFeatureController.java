package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminSubscriberFeatureService;

@RestController
@RequestMapping("/api/v1/admin/subscriber-features")
@RequiredArgsConstructor
public class AdminSubscriberFeatureController {

    private final AdminSubscriberFeatureService adminSubscriberFeatureService;

    @GetMapping
    public Page<SubscriberFeatureAdminResponse> getSubscriberFeatures(Pageable pageable) {
        return adminSubscriberFeatureService.getSubscriberFeatures(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberFeatureAdminResponse getSubscriberFeature(@PathVariable Long id) {
        return adminSubscriberFeatureService.getSubscriberFeature(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberFeatureAdminResponse> createSubscriberFeature(
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminSubscriberFeatureService.createSubscriberFeature(request));
    }

    @PutMapping("/{id}")
    public SubscriberFeatureAdminResponse updateSubscriberFeature(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return adminSubscriberFeatureService.updateSubscriberFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriberFeature(@PathVariable Long id) {
        adminSubscriberFeatureService.deleteSubscriberFeature(id);
        return ResponseEntity.noContent().build();
    }
}

