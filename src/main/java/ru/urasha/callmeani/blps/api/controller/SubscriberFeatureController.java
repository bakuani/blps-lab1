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
public class SubscriberFeatureController {

    private final SubscriberFeatureService subscriberFeatureService;

    @GetMapping
    public Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable) {
        return subscriberFeatureService.getSubscriberFeatures(pageable);
    }

    @GetMapping("/{id}")
    public SubscriberFeatureResponse getSubscriberFeature(@PathVariable Long id) {
        return subscriberFeatureService.getSubscriberFeature(id);
    }

    @PostMapping
    public ResponseEntity<SubscriberFeatureResponse> createSubscriberFeature(
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriberFeatureService.createSubscriberFeature(request));
    }

    @PutMapping("/{id}")
    public SubscriberFeatureResponse updateSubscriberFeature(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return subscriberFeatureService.updateSubscriberFeature(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscriberFeature(@PathVariable Long id) {
        subscriberFeatureService.deleteSubscriberFeature(id);
        return ResponseEntity.noContent().build();
    }
}

