package ru.urasha.callmeani.blps.service.feature.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.FeatureNotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;

import ru.urasha.callmeani.blps.mapper.FeatureMapper;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdditionalFeatureServiceImpl implements AdditionalFeatureService {

    private final AdditionalFeatureRepository additionalFeatureRepository;
    private final FeatureCategoryService featureCategoryService;
    private final FeatureMapper featureMapper;

    @Transactional(readOnly = true)
    public List<AdditionalFeatureResponse> getFeatures() {
        return additionalFeatureRepository.findAll().stream().map(featureMapper::toFeatureResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdditionalFeatureResponse getFeature(Long id) {
        return featureMapper.toFeatureResponse(getAdditionalFeatureEntity(id));
    }

    @Transactional
    public AdditionalFeatureResponse createFeature(AdditionalFeatureUpsertRequest request) {
        FeatureCategory category = getFeatureCategoryEntity(request.categoryId());
        AdditionalFeature feature = new AdditionalFeature();
        featureMapper.updateAdditionalFeature(feature, request);
        feature.setCategory(category);
        return featureMapper.toFeatureResponse(additionalFeatureRepository.save(feature));
    }

    @Transactional
    public AdditionalFeatureResponse updateFeature(Long id, AdditionalFeatureUpsertRequest request) {
        AdditionalFeature feature = getAdditionalFeatureEntity(id);
        FeatureCategory category = getFeatureCategoryEntity(request.categoryId());
        featureMapper.updateAdditionalFeature(feature, request);
        feature.setCategory(category);
        return featureMapper.toFeatureResponse(additionalFeatureRepository.save(feature));
    }

    @Transactional
    public void deleteFeature(Long id) {
        additionalFeatureRepository.delete(getAdditionalFeatureEntity(id));
    }

    public AdditionalFeature getAdditionalFeatureEntity(Long id) {
        return additionalFeatureRepository.findById(id).orElseThrow(() -> new FeatureNotFoundException(id));
    }

    private FeatureCategory getFeatureCategoryEntity(Long id) {
        return featureCategoryService.getFeatureCategoryEntity(id);
    }
}
