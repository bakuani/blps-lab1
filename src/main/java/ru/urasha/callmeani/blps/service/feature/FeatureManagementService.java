package ru.urasha.callmeani.blps.service.feature;

import ru.urasha.callmeani.blps.api.dto.feature.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureSummaryDto;

import java.util.List;

public interface FeatureManagementService {
    List<FeatureSummaryDto> findSubscriberFeatures(Long subscriberId, Long categoryId, String query);
    List<IdNameDto> getFeatureCategories();
    FeatureDetailsResponse getFeatureDetails(Long featureId);
    DisableFeatureResponse disableFeature(Long subscriberId, Long featureId);
}






