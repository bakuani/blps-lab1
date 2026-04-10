package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.service.AdminAdditionalFeatureService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminAdditionalFeatureServiceImpl implements AdminAdditionalFeatureService {

    private final AdditionalFeatureRepository AdditionalFeatureRepository;
    private final FeatureCategoryRepository FeatureCategoryRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<AdditionalFeatureAdminResponse> getFeatures() {
        return AdditionalFeatureRepository.findAll().stream().map(adminMapper::toServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdditionalFeatureAdminResponse getFeature(Long id) {
        return adminMapper.toServiceResponse(getServiceEntity(id));
    }

    @Transactional
    public AdditionalFeatureAdminResponse createFeature(AdditionalFeatureUpsertRequest request) {
        FeatureCategory category = getFeatureCategoryEntity(request.categoryId());
        AdditionalFeature service = new AdditionalFeature();
        adminMapper.updateAdditionalFeature(service, request);
        service.setCategory(category);
        return adminMapper.toServiceResponse(AdditionalFeatureRepository.save(service));
    }

    @Transactional
    public AdditionalFeatureAdminResponse updateFeature(Long id, AdditionalFeatureUpsertRequest request) {
        AdditionalFeature service = getServiceEntity(id);
        FeatureCategory category = getFeatureCategoryEntity(request.categoryId());
        adminMapper.updateAdditionalFeature(service, request);
        service.setCategory(category);
        return adminMapper.toServiceResponse(AdditionalFeatureRepository.save(service));
    }

    @Transactional
    public void deleteFeature(Long id) {
        AdditionalFeatureRepository.delete(getServiceEntity(id));
    }

    private AdditionalFeature getServiceEntity(Long id) {
        return AdditionalFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }

    private FeatureCategory getFeatureCategoryEntity(Long id) {
        return FeatureCategoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Service category not found: " + id));
    }
}

