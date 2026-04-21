package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffOptionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariff-options")
@RequiredArgsConstructor
public class TariffOptionManagementController {

    private final TariffOptionService TariffOptionService;

    @GetMapping
    public List<TariffOptionResponse> getTariffOptions() {
        return TariffOptionService.getTariffOptions();
    }

    @GetMapping("/{id}")
    public TariffOptionResponse getTariffOption(@PathVariable Long id) {
        return TariffOptionService.getTariffOption(id);
    }

    @PostMapping
    public ResponseEntity<TariffOptionResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffOptionService.createTariffOption(request));
    }

    @PutMapping("/{id}")
    public TariffOptionResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return TariffOptionService.updateTariffOption(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        TariffOptionService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }
}
