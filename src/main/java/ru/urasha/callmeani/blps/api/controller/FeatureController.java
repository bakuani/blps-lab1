package ru.urasha.callmeani.blps.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.FeatureSummaryDto;
import ru.urasha.callmeani.blps.service.FeatureManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FeatureController {

    private final FeatureManagementService FeatureManagementService;

    @GetMapping("/subscribers/{subscriberId}/features")
    public List<FeatureSummaryDto> findSubscriberFeatures(
        @PathVariable Long subscriberId,
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String query
    ) {
        return FeatureManagementService.findSubscriberFeatures(subscriberId, categoryId, query);
    }

    @GetMapping("/features/categories")
    public List<IdNameDto> getFeatureCategories() {
        return FeatureManagementService.getFeatureCategories();
    }

    @GetMapping("/features/{serviceId}")
    public FeatureDetailsResponse getServiceDetails(@PathVariable Long serviceId) {
        return FeatureManagementService.getServiceDetails(serviceId);
    }

    @PostMapping("/subscribers/{subscriberId}/features/{serviceId}/disable")
    public DisableFeatureResponse disableService(@PathVariable Long subscriberId, @PathVariable Long serviceId) {
        return FeatureManagementService.disableService(subscriberId, serviceId);
    }
}



