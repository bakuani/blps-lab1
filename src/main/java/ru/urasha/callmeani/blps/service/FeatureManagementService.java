package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.FeatureSummaryDto;

import java.util.List;

public interface FeatureManagementService {
    List<FeatureSummaryDto> findSubscriberFeatures(Long subscriberId, Long categoryId, String query);
    List<IdNameDto> getFeatureCategories();
    FeatureDetailsResponse getServiceDetails(Long serviceId);
    DisableFeatureResponse disableService(Long subscriberId, Long serviceId);
}



