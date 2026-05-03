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
import java.util.Locale;

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
        String normalizedQuery = query == null ? null : query.trim().toLowerCase(Locale.ROOT);
        List<SubscriberFeature> features = subscriberFeatureService.findBySubscriberIdAndStatus(
            subscriberId,
            SubscriberFeatureStatus.ACTIVE
        );

        return features.stream()
            .filter(item -> categoryId == null || item.getFeature().getCategory().getId().equals(categoryId))
            .filter(item -> normalizedQuery == null || normalizedQuery.isBlank()
                || item.getFeature().getName().toLowerCase(Locale.ROOT).contains(normalizedQuery))
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
        return featureMapper.toFeatureDetailsResponse(additionalFeatureService.getAdditionalFeatureEntity(featureId));
    }

    public DisableFeatureResponse disableFeature(Long subscriberId, Long featureId) {
        return businessTransactionTemplate.execute(status -> doDisableFeature(subscriberId, featureId));
    }

    private DisableFeatureResponse doDisableFeature(Long subscriberId, Long featureId) {
        Subscriber subscriber = subscriberService.getSubscriberEntity(subscriberId);

        SubscriberFeature subscriberFeature = subscriberFeatureService
            .findBySubscriberIdAndFeatureIdAndStatus(subscriberId, featureId, SubscriberFeatureStatus.ACTIVE)
            .orElse(null);

        if (subscriberFeature == null) {
            NotificationEvent notification = notificationService.createNotification(
                subscriber,
                NotificationType.SERVICE_DISABLE_ERROR,
                "Feature is not connected or already disabled.",
                false
            );
            return buildDisableFailureResponse(
                "Feature is not active for subscriber",
                featureId,
                notification
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

        return buildDisableSuccessResponse(
            "Feature disabled successfully",
            featureId,
            disableCall,
            notification
        );
    }

    private DisableFeatureResponse buildDisableFailureResponse(
        String message,
        Long featureId,
        NotificationEvent notification
    ) {
        return new DisableFeatureResponse(
            false,
            message,
            featureId,
            List.of(),
            notificationMapper.toNotificationDto(notification)
        );
    }

    private DisableFeatureResponse buildDisableSuccessResponse(
        String message,
        Long featureId,
        BillingTransaction billingTransaction,
        NotificationEvent notification
    ) {
        return new DisableFeatureResponse(
            true,
            message,
            featureId,
            List.of(billingMapper.toBillingTransactionDto(billingTransaction)),
            notificationMapper.toNotificationDto(notification)
        );
    }
}
