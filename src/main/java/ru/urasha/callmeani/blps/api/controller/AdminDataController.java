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
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalServiceAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.AdditionalServiceUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.admin.NameRequest;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberServiceAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberServiceUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.TariffAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.admin.TariffUpsertRequest;
import ru.urasha.callmeani.blps.service.AdminDataService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

    private final AdminDataService adminDataService;

    @GetMapping("/tariff-categories")
    public List<IdNameResponse> getTariffCategories() {
        return adminDataService.getTariffCategories();
    }

    @GetMapping("/tariff-categories/{id}")
    public IdNameResponse getTariffCategory(@PathVariable Long id) {
        return adminDataService.getTariffCategory(id);
    }

    @PostMapping("/tariff-categories")
    public ResponseEntity<IdNameResponse> createTariffCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createTariffCategory(request));
    }

    @PutMapping("/tariff-categories/{id}")
    public IdNameResponse updateTariffCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminDataService.updateTariffCategory(id, request);
    }

    @DeleteMapping("/tariff-categories/{id}")
    public ResponseEntity<Void> deleteTariffCategory(@PathVariable Long id) {
        adminDataService.deleteTariffCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/service-categories")
    public List<IdNameResponse> getServiceCategories() {
        return adminDataService.getServiceCategories();
    }

    @GetMapping("/service-categories/{id}")
    public IdNameResponse getServiceCategory(@PathVariable Long id) {
        return adminDataService.getServiceCategory(id);
    }

    @PostMapping("/service-categories")
    public ResponseEntity<IdNameResponse> createServiceCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createServiceCategory(request));
    }

    @PutMapping("/service-categories/{id}")
    public IdNameResponse updateServiceCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminDataService.updateServiceCategory(id, request);
    }

    @DeleteMapping("/service-categories/{id}")
    public ResponseEntity<Void> deleteServiceCategory(@PathVariable Long id) {
        adminDataService.deleteServiceCategory(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariffs")
    public List<TariffAdminResponse> getTariffs() {
        return adminDataService.getTariffs();
    }

    @GetMapping("/tariffs/{id}")
    public TariffAdminResponse getTariff(@PathVariable Long id) {
        return adminDataService.getTariff(id);
    }

    @PostMapping("/tariffs")
    public ResponseEntity<TariffAdminResponse> createTariff(@Valid @RequestBody TariffUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createTariff(request));
    }

    @PutMapping("/tariffs/{id}")
    public TariffAdminResponse updateTariff(@PathVariable Long id, @Valid @RequestBody TariffUpsertRequest request) {
        return adminDataService.updateTariff(id, request);
    }

    @DeleteMapping("/tariffs/{id}")
    public ResponseEntity<Void> deleteTariff(@PathVariable Long id) {
        adminDataService.deleteTariff(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/tariff-options")
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return adminDataService.getTariffOptions();
    }

    @GetMapping("/tariff-options/{id}")
    public TariffOptionAdminResponse getTariffOption(@PathVariable Long id) {
        return adminDataService.getTariffOption(id);
    }

    @PostMapping("/tariff-options")
    public ResponseEntity<TariffOptionAdminResponse> createTariffOption(@Valid @RequestBody TariffOptionUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createTariffOption(request));
    }

    @PutMapping("/tariff-options/{id}")
    public TariffOptionAdminResponse updateTariffOption(
        @PathVariable Long id,
        @Valid @RequestBody TariffOptionUpsertRequest request
    ) {
        return adminDataService.updateTariffOption(id, request);
    }

    @DeleteMapping("/tariff-options/{id}")
    public ResponseEntity<Void> deleteTariffOption(@PathVariable Long id) {
        adminDataService.deleteTariffOption(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/services")
    public List<AdditionalServiceAdminResponse> getServices() {
        return adminDataService.getServices();
    }

    @GetMapping("/services/{id}")
    public AdditionalServiceAdminResponse getService(@PathVariable Long id) {
        return adminDataService.getService(id);
    }

    @PostMapping("/services")
    public ResponseEntity<AdditionalServiceAdminResponse> createService(
        @Valid @RequestBody AdditionalServiceUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createService(request));
    }

    @PutMapping("/services/{id}")
    public AdditionalServiceAdminResponse updateService(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalServiceUpsertRequest request
    ) {
        return adminDataService.updateService(id, request);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        adminDataService.deleteService(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscribers")
    public Page<SubscriberAdminResponse> getSubscribers(Pageable pageable) {
        return adminDataService.getSubscribers(pageable);
    }

    @GetMapping("/subscribers/{id}")
    public SubscriberAdminResponse getSubscriber(@PathVariable Long id) {
        return adminDataService.getSubscriber(id);
    }

    @PostMapping("/subscribers")
    public ResponseEntity<SubscriberAdminResponse> createSubscriber(@Valid @RequestBody SubscriberUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createSubscriber(request));
    }

    @PutMapping("/subscribers/{id}")
    public SubscriberAdminResponse updateSubscriber(@PathVariable Long id, @Valid @RequestBody SubscriberUpsertRequest request) {
        return adminDataService.updateSubscriber(id, request);
    }

    @DeleteMapping("/subscribers/{id}")
    public ResponseEntity<Void> deleteSubscriber(@PathVariable Long id) {
        adminDataService.deleteSubscriber(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/subscriber-services")
    public Page<SubscriberServiceAdminResponse> getSubscriberServices(Pageable pageable) {
        return adminDataService.getSubscriberServices(pageable);
    }

    @GetMapping("/subscriber-services/{id}")
    public SubscriberServiceAdminResponse getSubscriberService(@PathVariable Long id) {
        return adminDataService.getSubscriberService(id);
    }

    @PostMapping("/subscriber-services")
    public ResponseEntity<SubscriberServiceAdminResponse> createSubscriberService(
        @Valid @RequestBody SubscriberServiceUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminDataService.createSubscriberService(request));
    }

    @PutMapping("/subscriber-services/{id}")
    public SubscriberServiceAdminResponse updateSubscriberService(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberServiceUpsertRequest request
    ) {
        return adminDataService.updateSubscriberService(id, request);
    }

    @DeleteMapping("/subscriber-services/{id}")
    public ResponseEntity<Void> deleteSubscriberService(@PathVariable Long id) {
        adminDataService.deleteSubscriberService(id);
        return ResponseEntity.noContent().build();
    }
}
