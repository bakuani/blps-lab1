package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;

@RestController
@RequestMapping("/api/v1/admin/subscriber-features")
@RequiredArgsConstructor
public class SubscriberFeatureManagementController {

    private final SubscriberFeatureService SubscriberFeatureService;

    @GetMapping
    public Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable) {
        return SubscriberFeatureService.getSubscriberFeatures(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberFeatureResponse getSubscriberFeature(@PathVariable Long id) {
        return SubscriberFeatureService.getSubscriberFeature(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberFeatureResponse> createSubscriberFeature(
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriberFeatureService.createSubscriberFeature(request));
    }

    @PutMapping("/{id}")
    public SubscriberFeatureResponse updateSubscriberFeature(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return SubscriberFeatureService.updateSubscriberFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriberFeature(@PathVariable Long id) {
        SubscriberFeatureService.deleteSubscriberFeature(id);
        return ResponseEntity.noContent().build();
    }
}
