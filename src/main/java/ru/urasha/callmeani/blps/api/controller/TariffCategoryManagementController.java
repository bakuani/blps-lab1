package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.service.tariff.TariffCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariff-categories")
@RequiredArgsConstructor
public class TariffCategoryManagementController {

    private final TariffCategoryService TariffCategoryService;

    @GetMapping
    public List<IdNameResponse> getTariffCategories() {
        return TariffCategoryService.getTariffCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return TariffCategoryService.getTariffCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffCategoryService.createTariffCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return TariffCategoryService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        TariffCategoryService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }
}
