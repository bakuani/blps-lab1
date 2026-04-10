package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionUpsertRequest;

import java.util.List;

public interface AdminTariffOptionService {
    List<TariffOptionAdminResponse> getTariffOptions();
    TariffOptionAdminResponse getTariffOption(Long id);
    TariffOptionAdminResponse createTariffOption(TariffOptionUpsertRequest request);
    TariffOptionAdminResponse updateTariffOption(Long id, TariffOptionUpsertRequest request);
    void deleteTariffOption(Long id);
}