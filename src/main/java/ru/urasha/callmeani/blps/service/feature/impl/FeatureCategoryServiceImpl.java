package ru.urasha.callmeani.blps.service.feature.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.api.exception.FeatureCategoryNotFoundException;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;
import ru.urasha.callmeani.blps.mapper.FeatureMapper;

import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureCategoryServiceImpl implements FeatureCategoryService {

    private final FeatureCategoryRepository featureCategoryRepository;
    private final FeatureMapper featureMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getFeatureCategories() {
        return featureCategoryRepository.findAll().stream().map(featureMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getFeatureCategory(Long id) {
        return featureMapper.toIdNameResponse(getFeatureCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createFeatureCategory(NameRequest request) {
        FeatureCategory category = new FeatureCategory();
        category.setName(request.name());
        return featureMapper.toIdNameResponse(featureCategoryRepository.save(category));
    }

    @Transactional
    public IdNameResponse updateFeatureCategory(Long id, NameRequest request) {
        FeatureCategory category = getFeatureCategoryEntity(id);
        category.setName(request.name());
        return featureMapper.toIdNameResponse(featureCategoryRepository.save(category));
    }

    @Transactional
    public void deleteFeatureCategory(Long id) {
        featureCategoryRepository.delete(getFeatureCategoryEntity(id));
    }

    public FeatureCategory getFeatureCategoryEntity(Long id) {
        return featureCategoryRepository.findById(id)
            .orElseThrow(() -> new FeatureCategoryNotFoundException(id));
    }

    public List<FeatureCategory> findAll() {
        return featureCategoryRepository.findAll();
    }
}
