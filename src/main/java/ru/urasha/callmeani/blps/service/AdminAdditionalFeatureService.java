package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureUpsertRequest;

import java.util.List;

public interface AdminAdditionalFeatureService {
    List<AdditionalFeatureAdminResponse> getFeatures();
    AdditionalFeatureAdminResponse getFeature(Long id);
    AdditionalFeatureAdminResponse createFeature(AdditionalFeatureUpsertRequest request);
    AdditionalFeatureAdminResponse updateFeature(Long id, AdditionalFeatureUpsertRequest request);
    void deleteFeature(Long id);
}

