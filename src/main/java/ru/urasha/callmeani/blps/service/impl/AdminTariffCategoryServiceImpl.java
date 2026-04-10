package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.service.AdminTariffCategoryService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTariffCategoryServiceImpl implements AdminTariffCategoryService {

    private final TariffCategoryRepository tariffCategoryRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getTariffCategories() {
        return tariffCategoryRepository.findAll().stream().map(adminMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getTariffCategory(Long id) {
        return adminMapper.toIdNameResponse(getTariffCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createTariffCategory(NameRequest request) {
        TariffCategory category = new TariffCategory();
        category.setName(request.name());
        return adminMapper.toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public IdNameResponse updateTariffCategory(Long id, NameRequest request) {
        TariffCategory category = getTariffCategoryEntity(id);
        category.setName(request.name());
        return adminMapper.toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public void deleteTariffCategory(Long id) {
        tariffCategoryRepository.delete(getTariffCategoryEntity(id));
    }

    private TariffCategory getTariffCategoryEntity(Long id) {
        return tariffCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tariff category not found: " + id));
    }
}