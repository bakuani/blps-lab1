package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariffs")
@RequiredArgsConstructor
public class TariffManagementController {

    private final TariffService TariffService;

    @GetMapping
    public List<TariffResponse> getTariffs() {
        return TariffService.getTariffs();
    }

    @GetMapping("/{id}")
    public TariffResponse getTariff(@PathVariable Long id) {
        return TariffService.getTariff(id);
    }

    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffService.createTariff(request));
    }

    @PutMapping("/{id}")
    public TariffResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return TariffService.updateTariff(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        TariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
