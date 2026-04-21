package ru.urasha.callmeani.blps.service.tariff.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;

import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.service.tariff.TariffCategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffCategoryServiceImpl implements TariffCategoryService {

    private final TariffCategoryRepository tariffCategoryRepository;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getTariffCategories() {
        return tariffCategoryRepository.findAll().stream().map(tariffMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getTariffCategory(Long id) {
        return tariffMapper.toIdNameResponse(getTariffCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createTariffCategory(NameRequest request) {
        TariffCategory category = new TariffCategory();
        category.setName(request.name());
        return tariffMapper.toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public IdNameResponse updateTariffCategory(Long id, NameRequest request) {
        TariffCategory category = getTariffCategoryEntity(id);
        category.setName(request.name());
        return tariffMapper.toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public void deleteTariffCategory(Long id) {
        tariffCategoryRepository.delete(getTariffCategoryEntity(id));
    }

    public TariffCategory getTariffCategoryEntity(Long id) {
        return tariffCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tariff category not found: " + id));
    }

    public java.util.List<ru.urasha.callmeani.blps.domain.entity.TariffCategory> findAll() {
        return tariffCategoryRepository.findAll();
    }
}







