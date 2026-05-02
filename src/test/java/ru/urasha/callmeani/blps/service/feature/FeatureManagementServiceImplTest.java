package ru.urasha.callmeani.blps.service.feature;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.feature.DisableFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.mapper.BillingMapper;
import ru.urasha.callmeani.blps.mapper.FeatureMapper;
import ru.urasha.callmeani.blps.mapper.NotificationMapper;
import ru.urasha.callmeani.blps.mapper.SubscriberMapper;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.feature.impl.FeatureManagementServiceImpl;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FeatureManagementServiceImplTest {

    private SubscriberService subscriberService;
    private SubscriberFeatureService subscriberFeatureService;
    private BillingService billingService;
    private NotificationService notificationService;
    private BillingMapper billingMapper;
    private NotificationMapper notificationMapper;
    private TransactionTemplate transactionTemplate;
    private FeatureManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriberService = Mockito.mock(SubscriberService.class);
        subscriberFeatureService = Mockito.mock(SubscriberFeatureService.class);
        AdditionalFeatureService additionalFeatureService = Mockito.mock(AdditionalFeatureService.class);
        FeatureCategoryService featureCategoryService = Mockito.mock(FeatureCategoryService.class);
        billingService = Mockito.mock(BillingService.class);
        notificationService = Mockito.mock(NotificationService.class);
        FeatureMapper featureMapper = Mockito.mock(FeatureMapper.class);
        SubscriberMapper subscriberMapper = Mockito.mock(SubscriberMapper.class);
        billingMapper = Mockito.mock(BillingMapper.class);
        notificationMapper = Mockito.mock(NotificationMapper.class);
        transactionTemplate = Mockito.mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<DisableFeatureResponse> callback =
                (TransactionCallback<DisableFeatureResponse>) invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        service = new FeatureManagementServiceImpl(
            subscriberService,
            subscriberFeatureService,
            additionalFeatureService,
            featureCategoryService,
            billingService,
            notificationService,
            featureMapper,
            subscriberMapper,
            billingMapper,
            notificationMapper,
            transactionTemplate
        );
    }

    @Test
    void disableFeatureMarksRelationAsDisabled() {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);

        AdditionalFeature feature = new AdditionalFeature();
        feature.setId(5L);

        SubscriberFeature subscriberFeature = new SubscriberFeature();
        subscriberFeature.setSubscriber(subscriber);
        subscriberFeature.setService(feature);
        subscriberFeature.setStatus(SubscriberFeatureStatus.ACTIVE);

        BillingTransaction billing = new BillingTransaction();
        NotificationEvent notification = new NotificationEvent();

        when(subscriberService.getSubscriberEntity(1L)).thenReturn(subscriber);
        when(subscriberFeatureService.findBySubscriberIdAndServiceIdAndStatus(1L, 5L, SubscriberFeatureStatus.ACTIVE))
            .thenReturn(Optional.of(subscriberFeature));
        when(billingService.createTransaction(any(), any(), any(BigDecimal.class), any())).thenReturn(billing);
        when(notificationService.createNotification(any(), any(), any(), anyBoolean())).thenReturn(notification);
        when(billingMapper.toBillingTransactionDto(billing)).thenReturn(
            new BillingTransactionDto("SERVICE_DISABLE", BigDecimal.ZERO, "Billing", OffsetDateTime.now())
        );
        when(notificationMapper.toNotificationDto(notification)).thenReturn(
            new NotificationDto("SERVICE_DISABLED", "Disabled", true, OffsetDateTime.now())
        );

        DisableFeatureResponse response = service.disableFeature(1L, 5L);

        ArgumentCaptor<SubscriberFeature> featureCaptor = ArgumentCaptor.forClass(SubscriberFeature.class);
        verify(subscriberFeatureService).save(featureCaptor.capture());
        verify(subscriberFeatureService, never()).delete(any());
        assertThat(featureCaptor.getValue().getStatus()).isEqualTo(SubscriberFeatureStatus.DISABLED);
        assertThat(featureCaptor.getValue().getDisabledAt()).isNotNull();
        assertThat(response.success()).isTrue();
        assertThat(response.billingTransactions()).hasSize(1);
    }

    @Test
    void disableFeatureReturnsBusinessErrorWhenFeatureMissing() {
        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);
        NotificationEvent notification = new NotificationEvent();

        when(subscriberService.getSubscriberEntity(anyLong())).thenReturn(subscriber);
        when(subscriberFeatureService.findBySubscriberIdAndServiceIdAndStatus(1L, 5L, SubscriberFeatureStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(notificationService.createNotification(any(), any(), any(), anyBoolean())).thenReturn(notification);
        when(notificationMapper.toNotificationDto(notification)).thenReturn(
            new NotificationDto("SERVICE_DISABLE_ERROR", "Not active", false, OffsetDateTime.now())
        );

        DisableFeatureResponse response = service.disableFeature(1L, 5L);

        verify(subscriberFeatureService, never()).save(any());
        verify(subscriberFeatureService, never()).delete(any());
        assertThat(response.success()).isFalse();
        assertThat(response.billingTransactions()).isEqualTo(List.of());
    }
}
