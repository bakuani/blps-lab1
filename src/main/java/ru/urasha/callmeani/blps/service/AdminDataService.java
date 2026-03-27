package ru.urasha.callmeani.blps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalService;
import ru.urasha.callmeani.blps.domain.entity.ServiceCategory;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberService;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;
import ru.urasha.callmeani.blps.repository.AdditionalServiceRepository;
import ru.urasha.callmeani.blps.repository.ServiceCategoryRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberServiceRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffOptionRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminDataService {

    private final TariffCategoryRepository tariffCategoryRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final TariffRepository tariffRepository;
    private final TariffOptionRepository tariffOptionRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final SubscriberRepository subscriberRepository;
    private final SubscriberServiceRepository subscriberServiceRepository;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getTariffCategories() {
        return tariffCategoryRepository.findAll().stream().map(this::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getTariffCategory(Long id) {
        return toIdNameResponse(getTariffCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createTariffCategory(NameRequest request) {
        TariffCategory category = new TariffCategory();
        category.setName(request.name());
        TariffCategory saved = tariffCategoryRepository.save(category);
        return toIdNameResponse(saved);
    }

    @Transactional
    public IdNameResponse updateTariffCategory(Long id, NameRequest request) {
        TariffCategory category = getTariffCategoryEntity(id);
        category.setName(request.name());
        return toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public void deleteTariffCategory(Long id) {
        TariffCategory category = getTariffCategoryEntity(id);
        tariffCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<IdNameResponse> getServiceCategories() {
        return serviceCategoryRepository.findAll().stream().map(this::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getServiceCategory(Long id) {
        return toIdNameResponse(getServiceCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createServiceCategory(NameRequest request) {
        ServiceCategory category = new ServiceCategory();
        category.setName(request.name());
        ServiceCategory saved = serviceCategoryRepository.save(category);
        return toIdNameResponse(saved);
    }

    @Transactional
    public IdNameResponse updateServiceCategory(Long id, NameRequest request) {
        ServiceCategory category = getServiceCategoryEntity(id);
        category.setName(request.name());
        return toIdNameResponse(serviceCategoryRepository.save(category));
    }

    @Transactional
    public void deleteServiceCategory(Long id) {
        ServiceCategory category = getServiceCategoryEntity(id);
        serviceCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<TariffAdminResponse> getTariffs() {
        return tariffRepository.findAll().stream().map(this::toTariffResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffAdminResponse getTariff(Long id) {
        return toTariffResponse(getTariffEntity(id));
    }

    @Transactional
    public TariffAdminResponse createTariff(TariffUpsertRequest request) {
        TariffCategory category = getTariffCategoryEntity(request.categoryId());

        Tariff tariff = new Tariff();
        applyTariffRequest(tariff, request, category);

        Tariff saved = tariffRepository.save(tariff);
        return toTariffResponse(saved);
    }

    @Transactional
    public TariffAdminResponse updateTariff(Long id, TariffUpsertRequest request) {
        Tariff tariff = getTariffEntity(id);
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        applyTariffRequest(tariff, request, category);
        return toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(Long id) {
        Tariff tariff = getTariffEntity(id);
        tariffRepository.delete(tariff);
    }

    @Transactional(readOnly = true)
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return tariffOptionRepository.findAll().stream().map(this::toTariffOptionResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffOptionAdminResponse getTariffOption(Long id) {
        return toTariffOptionResponse(getTariffOptionEntity(id));
    }

    @Transactional
    public TariffOptionAdminResponse createTariffOption(TariffOptionUpsertRequest request) {
        Tariff tariff = getTariffEntity(request.tariffId());

        TariffOption option = new TariffOption();
        applyTariffOptionRequest(option, request, tariff);

        TariffOption saved = tariffOptionRepository.save(option);
        return toTariffOptionResponse(saved);
    }

    @Transactional
    public TariffOptionAdminResponse updateTariffOption(Long id, TariffOptionUpsertRequest request) {
        TariffOption option = getTariffOptionEntity(id);
        Tariff tariff = getTariffEntity(request.tariffId());
        applyTariffOptionRequest(option, request, tariff);
        return toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public void deleteTariffOption(Long id) {
        TariffOption option = getTariffOptionEntity(id);
        tariffOptionRepository.delete(option);
    }

    @Transactional(readOnly = true)
    public List<AdditionalServiceAdminResponse> getServices() {
        return additionalServiceRepository.findAll().stream().map(this::toServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdditionalServiceAdminResponse getService(Long id) {
        return toServiceResponse(getServiceEntity(id));
    }

    @Transactional
    public AdditionalServiceAdminResponse createService(AdditionalServiceUpsertRequest request) {
        ServiceCategory category = getServiceCategoryEntity(request.categoryId());

        AdditionalService service = new AdditionalService();
        applyAdditionalServiceRequest(service, request, category);

        AdditionalService saved = additionalServiceRepository.save(service);
        return toServiceResponse(saved);
    }

    @Transactional
    public AdditionalServiceAdminResponse updateService(Long id, AdditionalServiceUpsertRequest request) {
        AdditionalService service = getServiceEntity(id);
        ServiceCategory category = getServiceCategoryEntity(request.categoryId());
        applyAdditionalServiceRequest(service, request, category);
        return toServiceResponse(additionalServiceRepository.save(service));
    }

    @Transactional
    public void deleteService(Long id) {
        AdditionalService service = getServiceEntity(id);
        additionalServiceRepository.delete(service);
    }

    @Transactional(readOnly = true)
    public List<SubscriberAdminResponse> getSubscribers() {
        return subscriberRepository.findAll().stream().map(this::toSubscriberResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriberAdminResponse getSubscriber(Long id) {
        return toSubscriberResponse(getSubscriberEntity(id));
    }

    @Transactional
    public SubscriberAdminResponse createSubscriber(SubscriberUpsertRequest request) {
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());

        Subscriber subscriber = new Subscriber();
        applySubscriberRequest(subscriber, request, tariff);

        Subscriber saved = subscriberRepository.save(subscriber);
        return toSubscriberResponse(saved);
    }

    @Transactional
    public SubscriberAdminResponse updateSubscriber(Long id, SubscriberUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(id);
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        applySubscriberRequest(subscriber, request, tariff);
        return toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        Subscriber subscriber = getSubscriberEntity(id);
        subscriberRepository.delete(subscriber);
    }

    @Transactional(readOnly = true)
    public List<SubscriberServiceAdminResponse> getSubscriberServices() {
        return subscriberServiceRepository.findAll().stream().map(this::toSubscriberServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public SubscriberServiceAdminResponse getSubscriberService(Long id) {
        return toSubscriberServiceResponse(getSubscriberServiceEntity(id));
    }

    @Transactional
    public SubscriberServiceAdminResponse createSubscriberService(SubscriberServiceUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalService service = getServiceEntity(request.serviceId());

        SubscriberService subscriberService = new SubscriberService();
        applySubscriberServiceRequest(subscriberService, request, subscriber, service);

        SubscriberService saved = subscriberServiceRepository.save(subscriberService);
        return toSubscriberServiceResponse(saved);
    }

    @Transactional
    public SubscriberServiceAdminResponse updateSubscriberService(Long id, SubscriberServiceUpsertRequest request) {
        SubscriberService subscriberService = getSubscriberServiceEntity(id);
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalService service = getServiceEntity(request.serviceId());
        applySubscriberServiceRequest(subscriberService, request, subscriber, service);
        return toSubscriberServiceResponse(subscriberServiceRepository.save(subscriberService));
    }

    @Transactional
    public void deleteSubscriberService(Long id) {
        SubscriberService subscriberService = getSubscriberServiceEntity(id);
        subscriberServiceRepository.delete(subscriberService);
    }

    private void applyTariffRequest(Tariff tariff, TariffUpsertRequest request, TariffCategory category) {
        tariff.setName(request.name());
        tariff.setDescription(request.description());
        tariff.setMonthlyFee(request.monthlyFee());
        tariff.setSwitchFee(request.switchFee());
        tariff.setCustomizable(request.customizable());
        tariff.setPdfUrl(request.pdfUrl());
        tariff.setCategory(category);
    }

    private void applyTariffOptionRequest(TariffOption option, TariffOptionUpsertRequest request, Tariff tariff) {
        option.setName(request.name());
        option.setDescription(request.description());
        option.setPrice(request.price());
        option.setTariff(tariff);
    }

    private void applyAdditionalServiceRequest(
        AdditionalService service,
        AdditionalServiceUpsertRequest request,
        ServiceCategory category
    ) {
        service.setName(request.name());
        service.setDescription(request.description());
        service.setMonthlyFee(request.monthlyFee());
        service.setCategory(category);
    }

    private void applySubscriberRequest(Subscriber subscriber, SubscriberUpsertRequest request, Tariff tariff) {
        subscriber.setPhone(request.phone());
        subscriber.setFullName(request.fullName());
        subscriber.setBalance(request.balance());
        subscriber.setCurrentTariff(tariff);
    }

    private void applySubscriberServiceRequest(
        SubscriberService subscriberService,
        SubscriberServiceUpsertRequest request,
        Subscriber subscriber,
        AdditionalService service
    ) {
        subscriberService.setSubscriber(subscriber);
        subscriberService.setService(service);
        subscriberService.setStatus(request.status());
        subscriberService.setConnectedAt(request.connectedAt() == null ? OffsetDateTime.now() : request.connectedAt());
        subscriberService.setDisabledAt(request.disabledAt());
    }

    private IdNameResponse toIdNameResponse(TariffCategory category) {
        return new IdNameResponse(category.getId(), category.getName());
    }

    private IdNameResponse toIdNameResponse(ServiceCategory category) {
        return new IdNameResponse(category.getId(), category.getName());
    }

    private TariffAdminResponse toTariffResponse(Tariff tariff) {
        return new TariffAdminResponse(
            tariff.getId(),
            tariff.getName(),
            tariff.getDescription(),
            tariff.getMonthlyFee(),
            tariff.getSwitchFee(),
            tariff.isCustomizable(),
            tariff.getPdfUrl(),
            tariff.getCategory().getId()
        );
    }

    private TariffOptionAdminResponse toTariffOptionResponse(TariffOption option) {
        return new TariffOptionAdminResponse(
            option.getId(),
            option.getName(),
            option.getDescription(),
            option.getPrice(),
            option.getTariff().getId()
        );
    }

    private AdditionalServiceAdminResponse toServiceResponse(AdditionalService service) {
        return new AdditionalServiceAdminResponse(
            service.getId(),
            service.getName(),
            service.getDescription(),
            service.getMonthlyFee(),
            service.getCategory().getId()
        );
    }

    private SubscriberAdminResponse toSubscriberResponse(Subscriber subscriber) {
        Long tariffId = subscriber.getCurrentTariff() == null ? null : subscriber.getCurrentTariff().getId();
        return new SubscriberAdminResponse(
            subscriber.getId(),
            subscriber.getPhone(),
            subscriber.getFullName(),
            subscriber.getBalance(),
            tariffId
        );
    }

    private SubscriberServiceAdminResponse toSubscriberServiceResponse(SubscriberService subscriberService) {
        return new SubscriberServiceAdminResponse(
            subscriberService.getId(),
            subscriberService.getSubscriber().getId(),
            subscriberService.getService().getId(),
            subscriberService.getStatus(),
            subscriberService.getConnectedAt(),
            subscriberService.getDisabledAt()
        );
    }

    private TariffCategory getTariffCategoryEntity(Long id) {
        return tariffCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tariff category not found: " + id));
    }

    private ServiceCategory getServiceCategoryEntity(Long id) {
        return serviceCategoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Service category not found: " + id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tariff not found: " + id));
    }

    private TariffOption getTariffOptionEntity(Long id) {
        return tariffOptionRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Tariff option not found: " + id));
    }

    private AdditionalService getServiceEntity(Long id) {
        return additionalServiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }

    private Subscriber getSubscriberEntity(Long id) {
        return subscriberRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Subscriber not found: " + id));
    }

    private SubscriberService getSubscriberServiceEntity(Long id) {
        return subscriberServiceRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Subscriber service not found: " + id));
    }
}
