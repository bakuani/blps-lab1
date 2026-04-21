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
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.AdditionalFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.common.NameRequest;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.TariffCategoryService;
import ru.urasha.callmeani.blps.service.tariff.TariffOptionService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class DataManagementController {

    private final TariffCategoryService TariffCategoryService;
    private final FeatureCategoryService FeatureCategoryService;
    private final TariffService TariffService;
    private final TariffOptionService TariffOptionService;
    private final AdditionalFeatureService AdditionalFeatureService;
    private final SubscriberService SubscriberService;
    private final SubscriberFeatureService SubscriberFeatureService;

    @GetMapping("/tariff-categories")
    public List<IdNameResponse> getTariffCategories() {
        return TariffCategoryService.getTariffCategories();
    }

    @GetMapping("/tariff-categories/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return TariffCategoryService.getTariffCategory(id);
    }

    @PostMapping("/tariff-categories")
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffCategoryService.createTariffCategory(request));
    }

    @PutMapping("/tariff-categories/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return TariffCategoryService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/tariff-categories/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        TariffCategoryService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/feature-categories")
    public List<IdNameResponse> getFeatureCategories() {
        return FeatureCategoryService.getFeatureCategories();
    }

    @GetMapping("/feature-categories/{id}")
    public IdNameResponse getFeatureCategory(@PathVariable Long id) {
        return FeatureCategoryService.getFeatureCategory(id);
    }

    @PostMapping("/feature-categories")
    public ResponseEntity<IdNameResponse> createFeatureCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(FeatureCategoryService.createFeatureCategory(request));
    }

    @PutMapping("/feature-categories/{id}")
    public IdNameResponse updateFeatureCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return FeatureCategoryService.updateFeatureCategory(id, request);
    }

    @DeleteMapping("/feature-categories/{id}")
    public ResponseEntity<Void> deleteFeatureCategory(@PathVariable Long id) {
        FeatureCategoryService.deleteFeatureCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariffs")
    public List<TariffResponse> getTariffs() {
        return TariffService.getTariffs();
    }

    @GetMapping("/tariffs/{id}")
    public TariffResponse getTariff(@PathVariable Long id) {
        return TariffService.getTariff(id);
    }

    @PostMapping("/tariffs")
    public ResponseEntity<TariffResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffService.createTariff(request));
    }

    @PutMapping("/tariffs/{id}")
    public TariffResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return TariffService.updateTariff(id, request);
    }

    @DeleteMapping("/tariffs/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        TariffService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariff-options")
    public List<TariffOptionResponse> getTariffOptions() {
        return TariffOptionService.getTariffOptions();
    }

    @GetMapping("/tariff-options/{id}")
    public TariffOptionResponse getTariffOption(@PathVariable Long id) {
        return TariffOptionService.getTariffOption(id);
    }

    @PostMapping("/tariff-options")
    public ResponseEntity<TariffOptionResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(TariffOptionService.createTariffOption(request));
    }

    @PutMapping("/tariff-options/{id}")
    public TariffOptionResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return TariffOptionService.updateTariffOption(id, request);
    }

    @DeleteMapping("/tariff-options/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        TariffOptionService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/features")
    public List<AdditionalFeatureResponse> getFeatures() {
        return AdditionalFeatureService.getFeatures();
    }

    @GetMapping("/features/{id}")
    public AdditionalFeatureResponse getFeature(@PathVariable Long id) {
        return AdditionalFeatureService.getFeature(id);
    }

    @PostMapping("/features")
    public ResponseEntity<AdditionalFeatureResponse> createFeature(
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(AdditionalFeatureService.createFeature(request));
    }

    @PutMapping("/features/{id}")
    public AdditionalFeatureResponse updateFeature(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalFeatureUpsertRequest request
    ) {
        return AdditionalFeatureService.updateFeature(id, request);
    }

    @DeleteMapping("/features/{id}")
    public ResponseEntity<Void> deleteFeature(@PathVariable Long id) {
        AdditionalFeatureService.deleteFeature(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscribers")
    public Page<SubscriberResponse> getSubscribers(Pageable pageable) {
        return SubscriberService.getSubscribers(pageable);
    }

    @GetMapping("/subscribers/{id}")
    public SubscriberResponse getSubscriber(@PathVariable Long id) {
        return SubscriberService.getSubscriber(id);
    }

    @PostMapping("/subscribers")
    public ResponseEntity<SubscriberResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriberService.createSubscriber(request));
    }

    @PutMapping("/subscribers/{id}")
    public SubscriberResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return SubscriberService.updateSubscriber(id, request);
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        SubscriberService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriber-features")
    public Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable) {
        return SubscriberFeatureService.getSubscriberFeatures(pageable);
    }

    @GetMapping("/subscriber-features/{id}")
    public SubscriberFeatureResponse getSubscriberFeature(@PathVariable Long id) {
        return SubscriberFeatureService.getSubscriberFeature(id);
    }

    @PostMapping("/subscriber-features")
    public ResponseEntity<SubscriberFeatureResponse> createSubscriberFeature(
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(SubscriberFeatureService.createSubscriberFeature(request));
    }

    @PutMapping("/subscriber-features/{id}")
    public SubscriberFeatureResponse updateSubscriberFeature(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberFeatureUpsertRequest request
    ) {
        return SubscriberFeatureService.updateSubscriberFeature(id, request);
    }

    @DeleteMapping("/subscriber-features/{id}")
    public ResponseEntity<Void> deleteSubscriberFeature(@PathVariable Long id) {
        SubscriberFeatureService.deleteSubscriberFeature(id);
        return ResponseEntity.noContent().build();
    }
}
