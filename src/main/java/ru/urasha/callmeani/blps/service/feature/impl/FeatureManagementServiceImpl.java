package ru.urasha.callmeani.blps.service.feature.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.feature.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.feature.FeatureSummaryDto;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.mapper.BillingMapper;
import ru.urasha.callmeani.blps.mapper.FeatureMapper;
import ru.urasha.callmeani.blps.mapper.NotificationMapper;
import ru.urasha.callmeani.blps.mapper.SubscriberMapper;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;
import ru.urasha.callmeani.blps.service.feature.FeatureCategoryService;
import ru.urasha.callmeani.blps.service.feature.FeatureManagementService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeatureManagementServiceImpl implements FeatureManagementService {

    private final SubscriberService subscriberService;
    private final SubscriberFeatureService subscriberFeatureService;
    private final AdditionalFeatureService additionalFeatureService;
    private final FeatureCategoryService featureCategoryService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final FeatureMapper featureMapper;
    private final SubscriberMapper subscriberMapper;
    private final BillingMapper billingMapper;
    private final NotificationMapper notificationMapper;
    @Qualifier("businessTransactionTemplate")
    private final TransactionTemplate businessTransactionTemplate;

    @Transactional(readOnly = true)
    public List<FeatureSummaryDto> findSubscriberFeatures(Long subscriberId, Long categoryId, String query) {
        List<SubscriberFeature> features = subscriberFeatureService.findBySubscriberIdAndStatus(
            subscriberId,
            SubscriberFeatureStatus.ACTIVE
        );

        return features.stream()
            .filter(item -> categoryId == null || item.getService().getCategory().getId().equals(categoryId))
            .filter(item -> query == null || query.isBlank() || item.getService().getName().toLowerCase().contains(query.toLowerCase()))
            .map(subscriberMapper::toFeatureSummaryDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<IdNameDto> getFeatureCategories() {
        return featureCategoryService.findAll()
            .stream()
            .map(featureMapper::toIdNameDto)
            .toList();
    }

    @Transactional(readOnly = true)
    public FeatureDetailsResponse getFeatureDetails(Long featureId) {
        AdditionalFeature feature = Optional.of(additionalFeatureService.getAdditionalFeatureEntity(featureId))
            .orElseThrow(() -> new NotFoundException("Feature not found: " + featureId));

        return featureMapper.toFeatureDetailsResponse(feature);
    }

    public DisableFeatureResponse disableFeature(Long subscriberId, Long featureId) {
        return businessTransactionTemplate.execute(status -> doDisableFeature(subscriberId, featureId));
    }

    private DisableFeatureResponse doDisableFeature(Long subscriberId, Long featureId) {
        Subscriber subscriber = Optional.of(subscriberService.getSubscriberEntity(subscriberId))
            .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));

        SubscriberFeature subscriberFeature = subscriberFeatureService
            .findBySubscriberIdAndServiceIdAndStatus(subscriberId, featureId, SubscriberFeatureStatus.ACTIVE)
            .orElse(null);

        if (subscriberFeature == null) {
            NotificationEvent notification = notificationService.createNotification(
                subscriber,
                NotificationType.SERVICE_DISABLE_ERROR,
                "Feature is not connected or already disabled.",
                false
            );
            return new DisableFeatureResponse(
                false,
                "Feature is not active for subscriber",
                featureId,
                List.of(),
                notificationMapper.toNotificationDto(notification)
            );
        }

        BillingTransaction disableCall = billingService.createTransaction(
            subscriber,
            BillingTransactionType.SERVICE_DISABLE,
            BigDecimal.ZERO,
            "Billing request for feature disable operation"
        );

        subscriberFeature.setStatus(SubscriberFeatureStatus.DISABLED);
        subscriberFeature.setDisabledAt(OffsetDateTime.now());
        subscriberFeatureService.save(subscriberFeature);

        NotificationEvent notification = notificationService.createNotification(
            subscriber,
            NotificationType.SERVICE_DISABLED,
            "Feature disabled successfully.",
            true
        );

        return new DisableFeatureResponse(
            true,
            "Feature disabled successfully",
            featureId,
            List.of(billingMapper.toBillingTransactionDto(disableCall)),
            notificationMapper.toNotificationDto(notification)
        );
    }
}
