package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;

import java.util.List;

public interface TariffOptionService {
    List<TariffOptionResponse> getTariffOptions();
    TariffOptionResponse getTariffOption(Long id);
    TariffOptionResponse createTariffOption(TariffOptionUpsertRequest request);
    TariffOptionResponse updateTariffOption(Long id, TariffOptionUpsertRequest request);
    void deleteTariffOption(Long id);
    ru.urasha.callmeani.blps.domain.entity.TariffOption getTariffOptionEntity(Long id);
}






