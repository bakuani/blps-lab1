package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffOptionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tariff-options")
@RequiredArgsConstructor
public class TariffOptionController {

    private final TariffOptionService tariffOptionService;

    @GetMapping
    public List<TariffOptionResponse> getTariffOptions() {
        return tariffOptionService.getTariffOptions();
    }

    @GetMapping("/{id}")
    public TariffOptionResponse getTariffOption(@PathVariable Long id) {
        return tariffOptionService.getTariffOption(id);
    }

    @PostMapping
    public ResponseEntity<TariffOptionResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffOptionService.createTariffOption(request));
    }

    @PutMapping("/{id}")
    public TariffOptionResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return tariffOptionService.updateTariffOption(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        tariffOptionService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }
}

