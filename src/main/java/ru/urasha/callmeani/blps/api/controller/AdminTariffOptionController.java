package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminTariffOptionService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariff-options")
@RequiredArgsConstructor
public class AdminTariffOptionController {

    private final AdminTariffOptionService adminTariffOptionService;

    @GetMapping
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return adminTariffOptionService.getTariffOptions();
    }

    @GetMapping("/{id}")
    public TariffOptionAdminResponse getTariffOption(@PathVariable Long id) {
        return adminTariffOptionService.getTariffOption(id);
    }

    @PostMapping
    public ResponseEntity<TariffOptionAdminResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffOptionService.createTariffOption(request));
    }

    @PutMapping("/{id}")
    public TariffOptionAdminResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return adminTariffOptionService.updateTariffOption(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        adminTariffOptionService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }
}
