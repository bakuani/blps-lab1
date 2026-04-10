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
import ru.urasha.callmeani.blps.service.AdminTariffCategoryService;
import ru.urasha.callmeani.blps.service.AdminServiceCategoryService;
import ru.urasha.callmeani.blps.service.AdminTariffService;
import ru.urasha.callmeani.blps.service.AdminTariffOptionService;
import ru.urasha.callmeani.blps.service.AdminAdditionalServiceService;
import ru.urasha.callmeani.blps.service.AdminSubscriberService;
import ru.urasha.callmeani.blps.service.AdminSubscriberServiceService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminDataController {

    private final AdminTariffCategoryService adminTariffCategoryService;
    private final AdminServiceCategoryService adminServiceCategoryService;
    private final AdminTariffService adminTariffService;
    private final AdminTariffOptionService adminTariffOptionService;
    private final AdminAdditionalServiceService adminAdditionalServiceService;
    private final AdminSubscriberService adminSubscriberService;
    private final AdminSubscriberServiceService adminSubscriberServiceService;

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

    @GetMapping("/service-categories")
    public List<IdNameResponse> getServiceCategories() {
        return adminServiceCategoryService.getServiceCategories();
    }

    @GetMapping("/service-categories/{id}")
    public IdNameResponse getServiceCategory(@PathVariable Long id) {
        return adminServiceCategoryService.getServiceCategory(id);
    }

    @PostMapping("/service-categories")
    public ResponseEntity<IdNameResponse> createServiceCategory(@Valid @RequestBody NameRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminServiceCategoryService.createServiceCategory(request));
    }

    @PutMapping("/service-categories/{id}")
    public IdNameResponse updateServiceCategory(@PathVariable Long id, @Valid @RequestBody NameRequest request) {
        return adminServiceCategoryService.updateServiceCategory(id, request);
    }

    @DeleteMapping("/service-categories/{id}")
    public ResponseEntity<Void> deleteServiceCategory(@PathVariable Long id) {
        adminServiceCategoryService.deleteServiceCategory(id);
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

    @GetMapping("/services")
    public List<AdditionalServiceAdminResponse> getServices() {
        return adminAdditionalServiceService.getServices();
    }

    @GetMapping("/services/{id}")
    public AdditionalServiceAdminResponse getService(@PathVariable Long id) {
        return adminAdditionalServiceService.getService(id);
    }

    @PostMapping("/services")
    public ResponseEntity<AdditionalServiceAdminResponse> createService(
        @Valid @RequestBody AdditionalServiceUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminAdditionalServiceService.createService(request));
    }

    @PutMapping("/services/{id}")
    public AdditionalServiceAdminResponse updateService(
        @PathVariable Long id,
        @Valid @RequestBody AdditionalServiceUpsertRequest request
    ) {
        return adminAdditionalServiceService.updateService(id, request);
    }

    @DeleteMapping("/services/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        adminAdditionalServiceService.deleteService(id);
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

    @GetMapping("/subscriber-services")
    public Page<SubscriberServiceAdminResponse> getSubscriberServices(Pageable pageable) {
        return adminSubscriberServiceService.getSubscriberServices(pageable);
    }

    @GetMapping("/subscriber-services/{id}")
    public SubscriberServiceAdminResponse getSubscriberService(@PathVariable Long id) {
        return adminSubscriberServiceService.getSubscriberService(id);
    }

    @PostMapping("/subscriber-services")
    public ResponseEntity<SubscriberServiceAdminResponse> createSubscriberService(
        @Valid @RequestBody SubscriberServiceUpsertRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminSubscriberServiceService.createSubscriberService(request));
    }

    @PutMapping("/subscriber-services/{id}")
    public SubscriberServiceAdminResponse updateSubscriberService(
        @PathVariable Long id,
        @Valid @RequestBody SubscriberServiceUpsertRequest request
    ) {
        return adminSubscriberServiceService.updateSubscriberService(id, request);
    }

    @DeleteMapping("/subscriber-services/{id}")
    public ResponseEntity<Void> deleteSubscriberService(@PathVariable Long id) {
        adminSubscriberServiceService.deleteSubscriberService(id);
        return ResponseEntity.noContent().build();
    }
}


