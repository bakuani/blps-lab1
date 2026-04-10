package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;

import java.util.List;

public interface AdminFeatureCategoryService {
    List<IdNameResponse> getFeatureCategories();
    IdNameResponse getFeatureCategory(Long id);
    IdNameResponse createFeatureCategory(NameRequest request);
    IdNameResponse updateFeatureCategory(Long id, NameRequest request);
    void deleteFeatureCategory(Long id);
}
