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
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.AdditionalServiceRepository;
import ru.urasha.callmeani.blps.repository.ServiceCategoryRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberServiceRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffOptionRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;

import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<IdNameResponse> getTariffCategories() {
        return tariffCategoryRepository.findAll().stream().map(adminMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getTariffCategory(Long id) {
        return adminMapper.toIdNameResponse(getTariffCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createTariffCategory(NameRequest request) {
        TariffCategory category = new TariffCategory();
        category.setName(request.name());
        TariffCategory saved = tariffCategoryRepository.save(category);
        return adminMapper.toIdNameResponse(saved);
    }

    @Transactional
    public IdNameResponse updateTariffCategory(Long id, NameRequest request) {
        TariffCategory category = getTariffCategoryEntity(id);
        category.setName(request.name());
        return adminMapper.toIdNameResponse(tariffCategoryRepository.save(category));
    }

    @Transactional
    public void deleteTariffCategory(Long id) {
        TariffCategory category = getTariffCategoryEntity(id);
        tariffCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<IdNameResponse> getServiceCategories() {
        return serviceCategoryRepository.findAll().stream().map(adminMapper::toIdNameResponse).toList();
    }

    @Transactional(readOnly = true)
    public IdNameResponse getServiceCategory(Long id) {
        return adminMapper.toIdNameResponse(getServiceCategoryEntity(id));
    }

    @Transactional
    public IdNameResponse createServiceCategory(NameRequest request) {
        ServiceCategory category = new ServiceCategory();
        category.setName(request.name());
        ServiceCategory saved = serviceCategoryRepository.save(category);
        return adminMapper.toIdNameResponse(saved);
    }

    @Transactional
    public IdNameResponse updateServiceCategory(Long id, NameRequest request) {
        ServiceCategory category = getServiceCategoryEntity(id);
        category.setName(request.name());
        return adminMapper.toIdNameResponse(serviceCategoryRepository.save(category));
    }

    @Transactional
    public void deleteServiceCategory(Long id) {
        ServiceCategory category = getServiceCategoryEntity(id);
        serviceCategoryRepository.delete(category);
    }

    @Transactional(readOnly = true)
    public List<TariffAdminResponse> getTariffs() {
        return tariffRepository.findAll().stream().map(adminMapper::toTariffResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffAdminResponse getTariff(Long id) {
        return adminMapper.toTariffResponse(getTariffEntity(id));
    }

    @Transactional
    public TariffAdminResponse createTariff(TariffUpsertRequest request) {
        TariffCategory category = getTariffCategoryEntity(request.categoryId());

        Tariff tariff = new Tariff();
        adminMapper.updateTariff(tariff, request);
        tariff.setCategory(category);

        Tariff saved = tariffRepository.save(tariff);
        return adminMapper.toTariffResponse(saved);
    }

    @Transactional
    public TariffAdminResponse updateTariff(Long id, TariffUpsertRequest request) {
        Tariff tariff = getTariffEntity(id);
        TariffCategory category = getTariffCategoryEntity(request.categoryId());
        
        adminMapper.updateTariff(tariff, request);
        tariff.setCategory(category);
        
        return adminMapper.toTariffResponse(tariffRepository.save(tariff));
    }

    @Transactional
    public void deleteTariff(Long id) {
        Tariff tariff = getTariffEntity(id);
        tariffRepository.delete(tariff);
    }

    @Transactional(readOnly = true)
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return tariffOptionRepository.findAll().stream().map(adminMapper::toTariffOptionResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffOptionAdminResponse getTariffOption(Long id) {
        return adminMapper.toTariffOptionResponse(getTariffOptionEntity(id));
    }

    @Transactional
    public TariffOptionAdminResponse createTariffOption(TariffOptionUpsertRequest request) {
        Tariff tariff = getTariffEntity(request.tariffId());

        TariffOption option = new TariffOption();
        adminMapper.updateTariffOption(option, request);
        option.setTariff(tariff);

        TariffOption saved = tariffOptionRepository.save(option);
        return adminMapper.toTariffOptionResponse(saved);
    }

    @Transactional
    public TariffOptionAdminResponse updateTariffOption(Long id, TariffOptionUpsertRequest request) {
        TariffOption option = getTariffOptionEntity(id);
        Tariff tariff = getTariffEntity(request.tariffId());
        
        adminMapper.updateTariffOption(option, request);
        option.setTariff(tariff);
        
        return adminMapper.toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public void deleteTariffOption(Long id) {
        TariffOption option = getTariffOptionEntity(id);
        tariffOptionRepository.delete(option);
    }

    @Transactional(readOnly = true)
    public List<AdditionalServiceAdminResponse> getServices() {
        return additionalServiceRepository.findAll().stream().map(adminMapper::toServiceResponse).toList();
    }

    @Transactional(readOnly = true)
    public AdditionalServiceAdminResponse getService(Long id) {
        return adminMapper.toServiceResponse(getServiceEntity(id));
    }

    @Transactional
    public AdditionalServiceAdminResponse createService(AdditionalServiceUpsertRequest request) {
        ServiceCategory category = getServiceCategoryEntity(request.categoryId());

        AdditionalService service = new AdditionalService();
        adminMapper.updateAdditionalService(service, request);
        service.setCategory(category);

        AdditionalService saved = additionalServiceRepository.save(service);
        return adminMapper.toServiceResponse(saved);
    }

    @Transactional
    public AdditionalServiceAdminResponse updateService(Long id, AdditionalServiceUpsertRequest request) {
        AdditionalService service = getServiceEntity(id);
        ServiceCategory category = getServiceCategoryEntity(request.categoryId());
        
        adminMapper.updateAdditionalService(service, request);
        service.setCategory(category);
        
        return adminMapper.toServiceResponse(additionalServiceRepository.save(service));
    }

    @Transactional
    public void deleteService(Long id) {
        AdditionalService service = getServiceEntity(id);
        additionalServiceRepository.delete(service);
    }

    @Transactional(readOnly = true)
    public Page<SubscriberAdminResponse> getSubscribers(Pageable pageable) {
        return subscriberRepository.findAll(pageable).map(adminMapper::toSubscriberResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberAdminResponse getSubscriber(Long id) {
        return adminMapper.toSubscriberResponse(getSubscriberEntity(id));
    }

    @Transactional
    public SubscriberAdminResponse createSubscriber(SubscriberUpsertRequest request) {
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());

        Subscriber subscriber = new Subscriber();
        adminMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);

        Subscriber saved = subscriberRepository.save(subscriber);
        return adminMapper.toSubscriberResponse(saved);
    }

    @Transactional
    public SubscriberAdminResponse updateSubscriber(Long id, SubscriberUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(id);
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        
        adminMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        
        return adminMapper.toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        Subscriber subscriber = getSubscriberEntity(id);
        subscriberRepository.delete(subscriber);
    }

    @Transactional(readOnly = true)
    public Page<SubscriberServiceAdminResponse> getSubscriberServices(Pageable pageable) {
        return subscriberServiceRepository.findAll(pageable).map(adminMapper::toSubscriberServiceResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberServiceAdminResponse getSubscriberService(Long id) {
        return adminMapper.toSubscriberServiceResponse(getSubscriberServiceEntity(id));
    }

    @Transactional
    public SubscriberServiceAdminResponse createSubscriberService(SubscriberServiceUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalService service = getServiceEntity(request.serviceId());

        SubscriberService subscriberService = new SubscriberService();
        adminMapper.updateSubscriberService(subscriberService, request);
        subscriberService.setSubscriber(subscriber);
        subscriberService.setService(service);
        if (subscriberService.getConnectedAt() == null) {
            subscriberService.setConnectedAt(OffsetDateTime.now());
        }

        SubscriberService saved = subscriberServiceRepository.save(subscriberService);
        return adminMapper.toSubscriberServiceResponse(saved);
    }

    @Transactional
    public SubscriberServiceAdminResponse updateSubscriberService(Long id, SubscriberServiceUpsertRequest request) {
        SubscriberService subscriberService = getSubscriberServiceEntity(id);
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalService service = getServiceEntity(request.serviceId());
        
        adminMapper.updateSubscriberService(subscriberService, request);
        subscriberService.setSubscriber(subscriber);
        subscriberService.setService(service);
        
        return adminMapper.toSubscriberServiceResponse(subscriberServiceRepository.save(subscriberService));
    }

    @Transactional
    public void deleteSubscriberService(Long id) {
        SubscriberService subscriberService = getSubscriberServiceEntity(id);
        subscriberServiceRepository.delete(subscriberService);
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
