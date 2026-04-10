package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.FeatureCategory;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.service.AdminFeatureCategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminFeatureCategoryServiceImpl implements AdminFeatureCategoryService {

    private final FeatureCategoryRepository FeatureCategoryRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getFeatureCategories() {
        return FeatureCategoryRepository.findAll().stream().map(adminMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getFeatureCategory(Long id) {
        return adminMapper.toIdNameResponse(getFeatureCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createFeatureCategory(NameRequest request) {
        FeatureCategory category = new FeatureCategory();
        category.setName(request.name());
        return adminMapper.toIdNameResponse(FeatureCategoryRepository.save(category));
    }

    @Transactional
    public IdNameResponse updateFeatureCategory(Long id, NameRequest request) {
        FeatureCategory category = getFeatureCategoryEntity(id);
        category.setName(request.name());
        return adminMapper.toIdNameResponse(FeatureCategoryRepository.save(category));
    }

    @Transactional
    public void deleteFeatureCategory(Long id) {
        FeatureCategoryRepository.delete(getFeatureCategoryEntity(id));
    }

    private FeatureCategory getFeatureCategoryEntity(Long id) {
        return FeatureCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Service category not found: " + id));
    }
}
