package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;

import java.util.List;

public interface TariffCategoryService {
    List<IdNameResponse> getTariffCategories();
    IdNameResponse getTariffCategory(Long id);
    IdNameResponse createTariffCategory(NameRequest request);
    IdNameResponse updateTariffCategory(Long id, NameRequest request);
    void deleteTariffCategory(Long id);
    TariffCategory getTariffCategoryEntity(Long id);
    List<TariffCategory> findAll();
}
