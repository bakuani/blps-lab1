package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.TariffAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.service.AdminTariffService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTariffServiceImpl implements AdminTariffService {

    private final TariffRepository tariffRepository;
    private final TariffCategoryRepository tariffCategoryRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<TariffAdminResponse> getTariffs() {
        return tariffRepository.findAll().stream().map(adminMapper::toTariffResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffAdminResponse getTariff(Long id) {
        return adminMapper.toTariffResponse(getTariffEntity(id));
    }

    @Transactional
    public TariffAdminResponse createTariff(TariffUpsertRequest request) {
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        Tariff tariff = new Tariff();
        adminMapper.updateTariff(tariff, request);
        tariff.setCategory(category);
        return adminMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public TariffAdminResponse updateTariff(Long id, TariffUpsertRequest request) {
        Tariff tariff = getTariffEntity(id);
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        adminMapper.updateTariff(tariff, request);
        tariff.setCategory(category);
        return adminMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(Long id) {
        tariffRepository.delete(getTariffEntity(id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff not found: " + id));
    }

    private TariffCategory getTariffCategoryEntity(Long id) {
        return tariffCategoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff category not found: " + id));
    }
}