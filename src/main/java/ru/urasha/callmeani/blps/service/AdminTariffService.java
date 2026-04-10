package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.admin.TariffAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffUpsertRequest;

import java.util.List;

public interface AdminTariffService {
    List<TariffAdminResponse> getTariffs();
    TariffAdminResponse getTariff(Long id);
    TariffAdminResponse createTariff(TariffUpsertRequest request);
    TariffAdminResponse updateTariff(Long id, TariffUpsertRequest request);
    void deleteTariff(Long id);
}