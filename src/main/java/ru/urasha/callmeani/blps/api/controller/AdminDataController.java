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
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.TariffAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminAdditionalFeatureService;
import ru.urasha.callmeani.blps.service.AdminFeatureCategoryService;
import ru.urasha.callmeani.blps.service.AdminSubscriberFeatureService;
import ru.urasha.callmeani.blps.service.AdminSubscriberService;
import ru.urasha.callmeani.blps.service.AdminTariffCategoryService;
import ru.urasha.callmeani.blps.service.AdminTariffOptionService;
import ru.urasha.callmeani.blps.service.AdminTariffService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

    private final AdminTariffCategoryService adminTariffCategoryService;
    private final AdminFeatureCategoryService adminFeatureCategoryService;
    private final AdminTariffService adminTariffService;
    private final AdminTariffOptionService adminTariffOptionService;
    private final AdminAdditionalFeatureService adminAdditionalFeatureService;
    private final AdminSubscriberService adminSubscriberService;
    private final AdminSubscriberFeatureService adminSubscriberFeatureService;

    @GetMapping("/tariff-categories")
    public List<IdNameResponse> getTariffCategories() {
        return adminTariffCategoryService.getTariffCategories();
    }

    @GetMapping("/tariff-categories/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return adminTariffCategoryService.getTariffCategory(id);
    }

    @PostMapping("/tariff-categories")
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffCategoryService.createTariffCategory(request));
    }

    @PutMapping("/tariff-categories/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminTariffCategoryService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/tariff-categories/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        adminTariffCategoryService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feature-categories")
    public List<IdNameResponse> getFeatureCategories() {
        return adminFeatureCategoryService.getFeatureCategories();
    }

    @GetMapping("/feature-categories/{id}")
    public IdNameResponse getFeatureCategory(@PathVariable Long id) {
        return adminFeatureCategoryService.getFeatureCategory(id);
    }

    @PostMapping("/feature-categories")
    public ResponseEntity<IdNameResponse> createFeatureCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminFeatureCategoryService.createFeatureCategory(request));
    }

    @PutMapping("/feature-categories/{id}")
    public IdNameResponse updateFeatureCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminFeatureCategoryService.updateFeatureCategory(id, request);
    }

    @DeleteMapping("/feature-categories/{id}")
    public ResponseEntity<Void> deleteFeatureCategory(@PathVariable Long id) {
        adminFeatureCategoryService.deleteFeatureCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariffs")
    public List<TariffAdminResponse> getTariffs() {
        return adminTariffService.getTariffs();
    }

    @GetMapping("/tariffs/{id}")
    public TariffAdminResponse getTariff(@PathVariable Long id) {
        return adminTariffService.getTariff(id);
    }

    @PostMapping("/tariffs")
    public ResponseEntity<TariffAdminResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffService.createTariff(request));
    }

    @PutMapping("/tariffs/{id}")
    public TariffAdminResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return adminTariffService.updateTariff(id, request);
    }

    @DeleteMapping("/tariffs/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        adminTariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariff-options")
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return adminTariffOptionService.getTariffOptions();
    }

    @GetMapping("/tariff-options/{id}")
    public TariffOptionAdminResponse getTariffOption(@PathVariable Long id) {
        return adminTariffOptionService.getTariffOption(id);
    }

    @PostMapping("/tariff-options")
    public ResponseEntity<TariffOptionAdminResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminTariffOptionService.createTariffOption(request));
    }

    @PutMapping("/tariff-options/{id}")
    public TariffOptionAdminResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return adminTariffOptionService.updateTariffOption(id, request);
    }

    @DeleteMapping("/tariff-options/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        adminTariffOptionService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/features")
    public List<AdditionalFeatureAdminResponse> getFeatures() {
        return adminAdditionalFeatureService.getFeatures();
    }

    @GetMapping("/features/{id}")
    public AdditionalFeatureAdminResponse getFeature(@PathVariable Long id) {
        return adminAdditionalFeatureService.getFeature(id);
    }

    @PostMapping("/features")
    public ResponseEntity<AdditionalFeatureAdminResponse> createFeature(
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAdditionalFeatureService.createFeature(request));
    }

    @PutMapping("/features/{id}")
    public AdditionalFeatureAdminResponse updateFeature(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return adminAdditionalFeatureService.updateFeature(id, request);
    }

    @DeleteMapping("/features/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        adminAdditionalFeatureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscribers")
    public Page<SubscriberAdminResponse> getSubscribers(Pageable pageable) {
        return adminSubscriberService.getSubscribers(pageable);
    }

    @GetMapping("/subscribers/{id}")
    public SubscriberAdminResponse getSubscriber(@PathVariable Long id) {
        return adminSubscriberService.getSubscriber(id);
    }

    @PostMapping("/subscribers")
    public ResponseEntity<SubscriberAdminResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminSubscriberService.createSubscriber(request));
    }

    @PutMapping("/subscribers/{id}")
    public SubscriberAdminResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return adminSubscriberService.updateSubscriber(id, request);
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        adminSubscriberService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriber-features")
    public Page<SubscriberFeatureAdminResponse> getSubscriberFeatures(Pageable pageable) {
        return adminSubscriberFeatureService.getSubscriberFeatures(pageable);
    }

    @GetMapping("/subscriber-features/{id}")
    public SubscriberFeatureAdminResponse getSubscriberFeature(@PathVariable Long id) {
        return adminSubscriberFeatureService.getSubscriberFeature(id);
    }

    @PostMapping("/subscriber-features")
    public ResponseEntity<SubscriberFeatureAdminResponse> createSubscriberFeature(
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminSubscriberFeatureService.createSubscriberFeature(request));
    }

    @PutMapping("/subscriber-features/{id}")
    public SubscriberFeatureAdminResponse updateSubscriberFeature(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return adminSubscriberFeatureService.updateSubscriberFeature(id, request);
    }

    @DeleteMapping("/subscriber-features/{id}")
    public ResponseEntity<Void> deleteSubscriberFeature(@PathVariable Long id) {
        adminSubscriberFeatureService.deleteSubscriberFeature(id);
        return ResponseEntity.noContent().build();
    }
}


