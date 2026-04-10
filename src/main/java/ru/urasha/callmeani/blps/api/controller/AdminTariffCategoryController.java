package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.service.AdminTariffCategoryService;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/tariff-categories")
@RequiredArgsConstructor
public class AdminTariffCategoryController {

    private final AdminTariffCategoryService adminTariffCategoryService;

    @GetMapping
    public List<IdNameResponse> getTariffCategories() {
        return adminTariffCategoryService.getTariffCategories();
    }

    @GetMapping("/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return adminTariffCategoryService.getTariffCategory(id);
    }

    @PostMapping
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffCategoryService.createTariffCategory(request));
    }

    @PutMapping("/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminTariffCategoryService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        adminTariffCategoryService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }
}
