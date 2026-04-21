package ru.urasha.callmeani.blps.service.tariff.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;

import ru.urasha.callmeani.blps.service.tariff.TariffCategoryService;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.service.tariff.TariffService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffServiceImpl implements TariffService {

    private final TariffRepository tariffRepository;
    private final TariffCategoryService tariffCategoryService;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public List<TariffResponse> getTariffs() {
        return tariffRepository.findAll().stream().map(tariffMapper::toTariffResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffResponse getTariff(Long id) {
        return tariffMapper.toTariffResponse(getTariffEntity(id));
    }

    @Transactional
    public TariffResponse createTariff(TariffUpsertRequest request) {
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        Tariff tariff = new Tariff();
        tariffMapper.updateTariff(tariff, request);
        tariff.setCategory(category);
        return tariffMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffResponse updateTariff(Long id, TariffUpsertRequest request) {
        Tariff tariff = getTariffEntity(id);
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        tariffMapper.updateTariff(tariff, request);
        tariff.setCategory(category);
        return tariffMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(Long id) {
        tariffRepository.delete(getTariffEntity(id));
    }

    public Tariff getTariffEntity(Long id) {
        return tariffRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff not found: " + id));
    }

    private TariffCategory getTariffCategoryEntity(Long id) {
        return tariffCategoryService.getTariffCategoryEntity(id);
    }

    public java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findAll() { return tariffRepository.findAll(); }
    public java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByCategoryId(Long categoryId) { return tariffRepository.findByCategoryId(categoryId); }
    public java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByNameContainingIgnoreCase(String query) { return tariffRepository.findByNameContainingIgnoreCase(query); }
    public java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String query) { return tariffRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, query); }
}









