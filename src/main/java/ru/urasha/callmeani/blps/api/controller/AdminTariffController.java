package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.TariffAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminTariffService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariffs")
@RequiredArgsConstructor
public class AdminTariffController {

    private final AdminTariffService adminTariffService;

    @GetMapping
    public List<TariffAdminResponse> getTariffs() {
        return adminTariffService.getTariffs();
    }

    @GetMapping("/{id}")
    public TariffAdminResponse getTariff(@PathVariable Long id) {
        return adminTariffService.getTariff(id);
    }

    @PostMapping
    public ResponseEntity<TariffAdminResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffService.createTariff(request));
    }

    @PutMapping("/{id}")
    public TariffAdminResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return adminTariffService.updateTariff(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        adminTariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }
}
