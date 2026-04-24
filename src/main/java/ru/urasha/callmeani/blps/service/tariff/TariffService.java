package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.domain.entity.Tariff;

import java.util.List;

public interface TariffService {
    List<TariffResponse> getTariffs();
    TariffResponse getTariff(Long id);
    TariffResponse createTariff(TariffUpsertRequest request);
    TariffResponse updateTariff(Long id, TariffUpsertRequest request);
    void deleteTariff(Long id);

    Tariff getTariffEntity(Long id);
    List<Tariff> findAll();
    List<Tariff> findByCategoryId(Long categoryId);
    List<Tariff> findByNameContainingIgnoreCase(String query);
    List<Tariff> findByCategoryIdAndNameContainingIgnoreCase(Long categoryId, String query);
}
