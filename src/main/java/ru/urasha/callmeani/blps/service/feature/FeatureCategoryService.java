package ru.urasha.callmeani.blps.service.feature;

import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;

import java.util.List;

public interface FeatureCategoryService {
    List<IdNameResponse> getFeatureCategories();
    IdNameResponse getFeatureCategory(Long id);
    IdNameResponse createFeatureCategory(NameRequest request);
    IdNameResponse updateFeatureCategory(Long id, NameRequest request);
    void deleteFeatureCategory(Long id);
    ru.urasha.callmeani.blps.domain.entity.FeatureCategory getFeatureCategoryEntity(Long id);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.FeatureCategory> findAll();
}





