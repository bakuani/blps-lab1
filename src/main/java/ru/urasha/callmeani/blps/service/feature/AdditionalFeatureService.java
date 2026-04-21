package ru.urasha.callmeani.blps.service.feature;

import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;

import java.util.List;

public interface AdditionalFeatureService {
    List<AdditionalFeatureResponse> getFeatures();
    AdditionalFeatureResponse getFeature(Long id);
    AdditionalFeatureResponse createFeature(AdditionalFeatureUpsertRequest request);
    AdditionalFeatureResponse updateFeature(Long id, AdditionalFeatureUpsertRequest request);
    void deleteFeature(Long id);
    ru.urasha.callmeani.blps.domain.entity.AdditionalFeature getAdditionalFeatureEntity(Long id);
}







