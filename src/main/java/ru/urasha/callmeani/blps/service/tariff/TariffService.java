package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;

import java.util.List;

public interface TariffService {
    List<TariffResponse> getTariffs();
    TariffResponse getTariff(Long id);
    TariffResponse createTariff(TariffUpsertRequest request);
    TariffResponse updateTariff(Long id, TariffUpsertRequest request);
    void deleteTariff(Long id);

    ru.urasha.callmeani.blps.domain.entity.Tariff getTariffEntity(Long id);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findAll();
    java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByCategoryId(Long categoryId);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByNameContainingIgnoreCase(String query);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.Tariff> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String query);
}





