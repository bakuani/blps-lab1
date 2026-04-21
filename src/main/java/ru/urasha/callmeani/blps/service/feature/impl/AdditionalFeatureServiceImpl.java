package ru.urasha.callmeani.blps.service.feature.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;

import ru.urasha.callmeani.blps.mapper.FeatureMapper;
import ru.urasha.callmeani.blps.mapper.SubscriberMapper;
import ru.urasha.callmeani.blps.mapper.TariffMapper;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdditionalFeatureServiceImpl implements AdditionalFeatureService {

    private final AdditionalFeatureRepository AdditionalFeatureRepository;
    private final FeatureCategoryService featureCategoryService;
    private final FeatureMapper featureMapper;
    private final SubscriberMapper subscriberMapper;
    private final TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public List<AdditionalFeatureResponse> getFeatures() {
        return AdditionalFeatureRepository.findAll().stream().map(featureMapper::toFeatureResponse).toList();
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
        return featureMapper.toFeatureResponse(AdditionalFeatureRepository.save(feature));
    }

    @Transactional
    public AdditionalFeatureResponse updateFeature(Long id, AdditionalFeatureUpsertRequest request) {
        AdditionalFeature feature = getAdditionalFeatureEntity(id);
        FeatureCategory category = getFeatureCategoryEntity(request.categoryId());
        featureMapper.updateAdditionalFeature(feature, request);
        feature.setCategory(category);
        return featureMapper.toFeatureResponse(AdditionalFeatureRepository.save(feature));
    }

    @Transactional
    public void deleteFeature(Long id) {
        AdditionalFeatureRepository.delete(getAdditionalFeatureEntity(id));
    }

    public AdditionalFeature getAdditionalFeatureEntity(Long id) {
        return AdditionalFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Feature not found: " + id));
    }

    private FeatureCategory getFeatureCategoryEntity(Long id) {
        return featureCategoryService.getFeatureCategoryEntity(id);
    }
}










