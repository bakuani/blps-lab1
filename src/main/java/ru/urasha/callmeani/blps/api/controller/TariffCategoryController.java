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
@RequestMapping("/api/v1/tariff-categories")
@RequiredArgsConstructor
public class TariffCategoryController {

    private final TariffCategoryService tariffCategoryService;

    @GetMapping
    public List<IdNameResponse> getTariffCategories() {
        return tariffCategoryService.getTariffCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return tariffCategoryService.getTariffCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tariffCategoryService.createTariffCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return tariffCategoryService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        tariffCategoryService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }
}

