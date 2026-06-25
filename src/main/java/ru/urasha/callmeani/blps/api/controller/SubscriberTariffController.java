package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeRequestStatusResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffChangeSubmissionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;
import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;
import ru.urasha.callmeani.blps.service.tariff.async.TariffChangeAsyncOperations;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class SubscriberTariffController {

    private final TariffManagementService tariffManagementService;
    private final TariffChangeAsyncOperations tariffChangeAsyncService;

    @GetMapping("/subscribers/{subscriberId}/tariff")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
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
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public ResponseEntity<TariffChangeSubmissionResponse> changeTariff(
        @PathVariable Long subscriberId,
        @Valid @RequestBody ChangeTariffRequest request
    ) {
        TariffChangeSubmissionResponse response = tariffChangeAsyncService.submitTariffChange(subscriberId, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/subscribers/{subscriberId}/tariff-change-requests/{requestId}")
    @PreAuthorize("@accessGuard.canAccessSubscriber(authentication, #p0)")
    public TariffChangeRequestStatusResponse getTariffChangeStatus(
        @PathVariable Long subscriberId,
        @PathVariable Long requestId
    ) {
        return tariffChangeAsyncService.getStatus(subscriberId, requestId);
    }
}
