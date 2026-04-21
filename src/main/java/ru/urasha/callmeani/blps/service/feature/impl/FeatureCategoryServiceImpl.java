package ru.urasha.callmeani.blps.service.feature.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;

import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FeatureCategoryServiceImpl implements FeatureCategoryService {

    private final FeatureCategoryRepository FeatureCategoryRepository;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getFeatureCategories() {
        return FeatureCategoryRepository.findAll().stream().map(featureMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getFeatureCategory(Long id) {
        return featureMapper.toIdNameResponse(getFeatureCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createFeatureCategory(NameRequest request) {
        FeatureCategory category = new FeatureCategory();
        category.setName(request.name());
        return featureMapper.toIdNameResponse(FeatureCategoryRepository.save(category));
    }

    @Transactional
    public IdNameResponse updateFeatureCategory(Long id, NameRequest request) {
        FeatureCategory category = getFeatureCategoryEntity(id);
        category.setName(request.name());
        return featureMapper.toIdNameResponse(FeatureCategoryRepository.save(category));
    }

    @Transactional
    public void deleteFeatureCategory(Long id) {
        FeatureCategoryRepository.delete(getFeatureCategoryEntity(id));
    }

    public FeatureCategory getFeatureCategoryEntity(Long id) {
        return FeatureCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Service category not found: " + id));
    }

    public java.util.List<ru.urasha.callmeani.blps.domain.entity.FeatureCategory> findAll() {
        return FeatureCategoryRepository.findAll();
    }
}







