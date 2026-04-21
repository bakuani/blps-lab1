package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;

import java.util.List;

public interface TariffCategoryService {
    List<IdNameResponse> getTariffCategories();
    IdNameResponse getTariffCategory(Long id);
    IdNameResponse createTariffCategory(NameRequest request);
    IdNameResponse updateTariffCategory(Long id, NameRequest request);
    void deleteTariffCategory(Long id);
    ru.urasha.callmeani.blps.domain.entity.TariffCategory getTariffCategoryEntity(Long id);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.TariffCategory> findAll();
}




