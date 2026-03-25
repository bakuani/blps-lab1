package ru.urasha.callmeani.blps.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.DisableServiceResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.NotificationDto;
import ru.urasha.callmeani.blps.api.dto.ServiceDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.ServiceSummaryDto;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalService;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberService;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.SubscriberServiceStatus;
import ru.urasha.callmeani.blps.repository.AdditionalServiceRepository;
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.repository.NotificationEventRepository;
import ru.urasha.callmeani.blps.repository.ServiceCategoryRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberServiceRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ServiceManagementService {

    private final SubscriberRepository subscriberRepository;
    private final SubscriberServiceRepository subscriberServiceRepository;
    private final AdditionalServiceRepository additionalServiceRepository;
    private final ServiceCategoryRepository serviceCategoryRepository;
    private final BillingTransactionRepository billingTransactionRepository;
    private final NotificationEventRepository notificationEventRepository;

    @Transactional(readOnly = true)
    public List<ServiceSummaryDto> findSubscriberServices(Long subscriberId, Long categoryId, String query) {
        List<SubscriberService> services = subscriberServiceRepository.findBySubscriberIdAndStatus(
            subscriberId,
            SubscriberServiceStatus.ACTIVE
        );

        return services.stream()
            .filter(item -> categoryId == null || item.getService().getCategory().getId().equals(categoryId))
            .filter(item -> query == null || query.isBlank() || item.getService().getName().toLowerCase().contains(query.toLowerCase()))
            .map(item -> new ServiceSummaryDto(
                item.getService().getId(),
                item.getService().getName(),
                item.getService().getCategory().getName(),
                item.getService().getMonthlyFee(),
                item.getStatus().name()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdNameDto> getServiceCategories() {
        return serviceCategoryRepository.findAll()
            .stream()
            .map(c -> new IdNameDto(c.getId(), c.getName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public ServiceDetailsResponse getServiceDetails(Long serviceId) {
        AdditionalService service = additionalServiceRepository.findById(serviceId)
            .orElseThrow(() -> new NotFoundException("Service not found: " + serviceId));

        return new ServiceDetailsResponse(
            service.getId(),
            service.getName(),
            service.getDescription(),
            service.getCategory().getName(),
            service.getMonthlyFee()
        );
    }

    @Transactional
    public DisableServiceResponse disableService(Long subscriberId, Long serviceId) {
        Subscriber subscriber = subscriberRepository.findById(subscriberId)
            .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));

        SubscriberService subscriberService = subscriberServiceRepository
            .findBySubscriberIdAndServiceIdAndStatus(subscriberId, serviceId, SubscriberServiceStatus.ACTIVE)
            .orElse(null);

        if (subscriberService == null) {
            NotificationEvent notification = saveNotification(
                subscriber,
                NotificationType.SERVICE_DISABLE_ERROR,
                "Услуга не подключена или уже отключена.",
                false
            );
            return new DisableServiceResponse(
                false,
                "Service is not active for subscriber",
                serviceId,
                List.of(),
                toNotificationDto(notification)
            );
        }

        BillingTransaction disableCall = saveBilling(
            subscriber,
            BillingTransactionType.SERVICE_DISABLE,
            BigDecimal.ZERO,
            "Запрос в биллинг на отключение услуги"
        );

        subscriberService.setStatus(SubscriberServiceStatus.DISABLED);
        subscriberService.setDisabledAt(OffsetDateTime.now());
        subscriberServiceRepository.delete(subscriberService);

        NotificationEvent notification = saveNotification(
            subscriber,
            NotificationType.SERVICE_DISABLED,
            "Услуга успешно отключена.",
            true
        );

        return new DisableServiceResponse(
            true,
            "Service disabled successfully",
            serviceId,
            List.of(toBillingDto(disableCall)),
            toNotificationDto(notification)
        );
    }

    private BillingTransaction saveBilling(Subscriber subscriber, BillingTransactionType type, BigDecimal amount, String description) {
        BillingTransaction transaction = new BillingTransaction();
        transaction.setSubscriber(subscriber);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setCreatedAt(OffsetDateTime.now());
        return billingTransactionRepository.save(transaction);
    }

    private NotificationEvent saveNotification(Subscriber subscriber, NotificationType type, String message, boolean success) {
        NotificationEvent notification = new NotificationEvent();
        notification.setSubscriber(subscriber);
        notification.setType(type);
        notification.setMessage(message);
        notification.setSuccess(success);
        notification.setCreatedAt(OffsetDateTime.now());
        return notificationEventRepository.save(notification);
    }

    private BillingTransactionDto toBillingDto(BillingTransaction transaction) {
        return new BillingTransactionDto(
            transaction.getType().name(),
            transaction.getAmount(),
            transaction.getDescription(),
            transaction.getCreatedAt()
        );
    }

    private NotificationDto toNotificationDto(NotificationEvent notification) {
        return new NotificationDto(
            notification.getType().name(),
            notification.getMessage(),
            notification.isSuccess(),
            notification.getCreatedAt()
        );
    }
}
