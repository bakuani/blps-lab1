package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.NotificationDto;
import ru.urasha.callmeani.blps.api.dto.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.FeatureSummaryDto;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.repository.FeatureCategoryRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberFeatureRepository;
import ru.urasha.callmeani.blps.mapper.ClientMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import ru.urasha.callmeani.blps.service.FeatureManagementService;
import ru.urasha.callmeani.blps.service.BillingService;
import ru.urasha.callmeani.blps.service.NotificationService;

@Service
@RequiredArgsConstructor
public class FeatureManagementServiceImpl implements FeatureManagementService {

    // todo: декомпозировать и создать сервисы под сущности + оформить интерфейсы для сервисов
    
    private final SubscriberRepository subscriberRepository;
    private final SubscriberFeatureRepository SubscriberFeatureRepository;
    private final AdditionalFeatureRepository AdditionalFeatureRepository;
    private final FeatureCategoryRepository FeatureCategoryRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public List<FeatureSummaryDto> findSubscriberFeatures(Long subscriberId, Long categoryId, String query) {
        List<SubscriberFeature> services = SubscriberFeatureRepository.findBySubscriberIdAndStatus(
            subscriberId,
            SubscriberFeatureStatus.ACTIVE
        );

        return services.stream()
            .filter(item -> categoryId == null || item.getService().getCategory().getId().equals(categoryId))
            .filter(item -> query == null || query.isBlank() || item.getService().getName().toLowerCase().contains(query.toLowerCase()))
            .map(clientMapper::toFeatureSummaryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdNameDto> getFeatureCategories() {
        return FeatureCategoryRepository.findAll()
            .stream()
            .map(clientMapper::toIdNameDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public FeatureDetailsResponse getServiceDetails(Long serviceId) {
        AdditionalFeature service = AdditionalFeatureRepository.findById(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));

        return clientMapper.toFeatureDetailsResponse(service);
    }

    @Transactional
    public DisableFeatureResponse disableService(Long subscriberId, Long serviceId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
            .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));

        SubscriberFeature SubscriberFeature = SubscriberFeatureRepository
            .findBySubscriberIdAndServiceIdAndStatus(subscriberId, serviceId, SubscriberFeatureStatus.ACTIVE)
            .orElse(null);

        if (SubscriberFeature == null) {
            NotificationEvent notification = notificationService.createNotification(
                subscriber,
                NotificationType.SERVICE_DISABLE_ERROR,
                "Услуга не подключена или уже отключена.",
                false
            );
            return new DisableFeatureResponse(
                false,
                "Service is not active for subscriber",
                serviceId,
                List.of(),
                clientMapper.toNotificationDto(notification)
            );
        }

        BillingTransaction disableCall = billingService.createTransaction(
            subscriber,
            BillingTransactionType.SERVICE_DISABLE,
            BigDecimal.ZERO,
            "Запрос в биллинг на отключение услуги"
        );

        SubscriberFeature.setStatus(SubscriberFeatureStatus.DISABLED);
        SubscriberFeature.setDisabledAt(OffsetDateTime.now());
        SubscriberFeatureRepository.delete(SubscriberFeature);

        NotificationEvent notification = notificationService.createNotification(
            subscriber,
            NotificationType.SERVICE_DISABLED,
            "Услуга успешно отключена.",
            true
        );

        return new DisableFeatureResponse(
            true,
            "Service disabled successfully",
            serviceId,
            List.of(clientMapper.toBillingTransactionDto(disableCall)),
            clientMapper.toNotificationDto(notification)
        );
    }
}



