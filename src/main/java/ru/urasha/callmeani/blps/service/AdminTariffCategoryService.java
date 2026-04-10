package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;

import java.util.List;

public interface AdminTariffCategoryService {
    List<IdNameResponse> getTariffCategories();
    IdNameResponse getTariffCategory(Long id);
    IdNameResponse createTariffCategory(NameRequest request);
    IdNameResponse updateTariffCategory(Long id, NameRequest request);
    void deleteTariffCategory(Long id);
}