package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.api.dto.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.TariffSummaryDto;

import java.util.List;

public interface TariffManagementService {
    TariffInfoResponse getSubscriberTariffInfo(Long subscriberId);
    List<IdNameDto> getTariffCategories();
    List<TariffSummaryDto> findTariffs(Long categoryId, String query);
    TariffDetailsResponse getTariffDetails(Long tariffId);
    ChangeTariffResponse changeTariff(Long subscriberId, ChangeTariffRequest request);
}
