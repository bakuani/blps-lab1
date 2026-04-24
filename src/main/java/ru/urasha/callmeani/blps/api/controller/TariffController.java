package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class TariffController {

    private final TariffManagementService tariffManagementService;
    private final TariffService tariffService;

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

    @GetMapping("/admin/tariffs")
    public List<TariffResponse> getAdminTariffs() {
        return tariffService.getTariffs();
    }

    @GetMapping("/admin/tariffs/{id}")
    public TariffResponse getAdminTariff(@PathVariable Long id) {
        return tariffService.getTariff(id);
    }

    @PostMapping("/admin/tariffs")
    public ResponseEntity<TariffResponse> createAdminTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffService.createTariff(request));
    }

    @PutMapping("/admin/tariffs/{id}")
    public TariffResponse updateAdminTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return tariffService.updateTariff(id, request);
    }

    @DeleteMapping("/admin/tariffs/{id}")
    public ResponseEntity<Void> deleteAdminTariff(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
