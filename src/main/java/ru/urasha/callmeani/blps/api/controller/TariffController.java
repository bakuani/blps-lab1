package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.TariffSummaryDto;
import ru.urasha.callmeani.blps.service.TariffManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class TariffController {

    private final TariffManagementService tariffManagementService;

    public TariffController(TariffManagementService tariffManagementService) {
        this.tariffManagementService = tariffManagementService;
    }

    @GetMapping("/subscribers/{subscriberId}/tariff")
    public TariffInfoResponse getSubscriberTariffInfo(@PathVariable Long subscriberId) {
        return tariffManagementService.getSubscriberTariffInfo(subscriberId);
    }

    @GetMapping("/tariffs/categories")
    public List<IdNameDto> getTariffCategories() {
        return tariffManagementService.getTariffCategories();
    }

    @GetMapping("/tariffs")
    public List<TariffSummaryDto> findTariffs(
        @RequestParam(required = false) Long categoryId,
        @RequestParam(required = false) String query
    ) {
        return tariffManagementService.findTariffs(categoryId, query);
    }

    @GetMapping("/tariffs/{tariffId}")
    public TariffDetailsResponse getTariffDetails(@PathVariable Long tariffId) {
        return tariffManagementService.getTariffDetails(tariffId);
    }

    @PostMapping("/subscribers/{subscriberId}/tariff/change")
    public ChangeTariffResponse changeTariff(
        @PathVariable Long subscriberId,
        @Valid @RequestBody ChangeTariffRequest request
    ) {
        return tariffManagementService.changeTariff(subscriberId, request);
    }
}
