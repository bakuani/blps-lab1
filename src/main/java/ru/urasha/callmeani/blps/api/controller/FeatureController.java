package ru.urasha.callmeani.blps.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDisableSubmissionResponse;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureSummaryDto;
import ru.urasha.callmeani.blps.service.feature.async.FeatureDisableAsyncService;
import ru.urasha.callmeani.blps.service.feature.FeatureManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureManagementService featureManagementService;
    private final FeatureDisableAsyncService featureDisableAsyncService;

    @GetMapping("/subscribers/{subscriberId}/features")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public List<FeatureSummaryDto> findSubscriberFeatures(
        @PathVariable Long subscriberId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String query
    ) {
        return featureManagementService.findSubscriberFeatures(subscriberId, categoryId, query);
    }

    @GetMapping("/features/categories")
    public List<IdNameDto> getFeatureCategories() {
        return featureManagementService.getFeatureCategories();
    }

    @GetMapping("/features/{featureId}")
    public FeatureDetailsResponse getFeatureDetails(@PathVariable Long featureId) {
        return featureManagementService.getFeatureDetails(featureId);
    }

    @PostMapping("/subscribers/{subscriberId}/features/{featureId}/disable")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public ResponseEntity<FeatureDisableSubmissionResponse> disableFeature(
        @PathVariable Long subscriberId,
        @PathVariable Long featureId
    ) {
        FeatureDisableSubmissionResponse response = featureDisableAsyncService.submitFeatureDisable(subscriberId, featureId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/subscribers/{subscriberId}/feature-disable-requests/{requestId}")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public FeatureDisableRequestStatusResponse getFeatureDisableStatus(
        @PathVariable Long subscriberId,
        @PathVariable Long requestId
    ) {
        return featureDisableAsyncService.getStatus(subscriberId, requestId);
    }
}
