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
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tariff-catalog")
@RequiredArgsConstructor
public class TariffCatalogController {

    private final TariffService tariffService;

    @GetMapping
    public List<TariffResponse> getTariffs() {
        return tariffService.getTariffs();
    }

    @GetMapping("/{id}")
    public TariffResponse getTariff(@PathVariable Long id) {
        return tariffService.getTariff(id);
    }

    @PostMapping
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffService.createTariff(request));
    }

    @PutMapping("/{id}")
    public TariffResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return tariffService.updateTariff(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        tariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
