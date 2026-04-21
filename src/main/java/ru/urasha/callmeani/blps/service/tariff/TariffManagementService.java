package ru.urasha.callmeani.blps.service.tariff;

import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;

import java.util.List;

public interface TariffManagementService {
    TariffInfoResponse getSubscriberTariffInfo(Long subscriberId);
    List<IdNameDto> getTariffCategories();
    List<TariffSummaryDto> findTariffs(Long categoryId, String query);
    TariffDetailsResponse getTariffDetails(Long tariffId);
    ChangeTariffResponse changeTariff(Long subscriberId, ChangeTariffRequest request);
}



