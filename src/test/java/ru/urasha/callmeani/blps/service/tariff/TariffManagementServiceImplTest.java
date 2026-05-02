package ru.urasha.callmeani.blps.service.tariff;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.mapper.BillingMapper;
import ru.urasha.callmeani.blps.mapper.NotificationMapper;
import ru.urasha.callmeani.blps.mapper.TariffMapper;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.impl.TariffManagementServiceImpl;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TariffManagementServiceImplTest {

    private SubscriberService subscriberService;
    private TariffService tariffService;
    private BillingService billingService;
    private NotificationService notificationService;
    private TariffMapper tariffMapper;
    private BillingMapper billingMapper;
    private NotificationMapper notificationMapper;
    private TransactionTemplate transactionTemplate;
    private TariffManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        subscriberService = Mockito.mock(SubscriberService.class);
        tariffService = Mockito.mock(TariffService.class);
        TariffCategoryService tariffCategoryService = Mockito.mock(TariffCategoryService.class);
        billingService = Mockito.mock(BillingService.class);
        notificationService = Mockito.mock(NotificationService.class);
        tariffMapper = Mockito.mock(TariffMapper.class);
        billingMapper = Mockito.mock(BillingMapper.class);
        notificationMapper = Mockito.mock(NotificationMapper.class);
        transactionTemplate = Mockito.mock(TransactionTemplate.class);

        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            TransactionCallback<ChangeTariffResponse> callback =
                (TransactionCallback<ChangeTariffResponse>) invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        when(tariffMapper.toTariffSummaryDto(any())).thenAnswer(invocation -> {
            Tariff tariff = invocation.getArgument(0);
            if (tariff == null) {
                return null;
            }
            return new TariffSummaryDto(tariff.getId(), tariff.getName(), "cat", tariff.getMonthlyFee());
        });

        service = new TariffManagementServiceImpl(
            subscriberService,
            tariffService,
            tariffCategoryService,
            billingService,
            notificationService,
            tariffMapper,
            billingMapper,
            notificationMapper,
            transactionTemplate
        );
    }

    @Test
    void changeTariffExecutesProgrammaticTransactionAndUpdatesBalance() {
        Tariff currentTariff = new Tariff();
        currentTariff.setId(1L);
        currentTariff.setName("Current");
        currentTariff.setMonthlyFee(BigDecimal.valueOf(5));
        currentTariff.setSwitchFee(BigDecimal.ZERO);

        Tariff targetTariff = new Tariff();
        targetTariff.setId(2L);
        targetTariff.setName("Target");
        targetTariff.setMonthlyFee(BigDecimal.valueOf(10));
        targetTariff.setSwitchFee(BigDecimal.valueOf(5));

        Subscriber subscriber = new Subscriber();
        subscriber.setId(1L);
        subscriber.setBalance(BigDecimal.valueOf(100));
        subscriber.setCurrentTariff(currentTariff);

        NotificationEvent notification = new NotificationEvent();

        when(subscriberService.getSubscriberEntity(1L)).thenReturn(subscriber);
        when(tariffService.getTariffEntity(2L)).thenReturn(targetTariff);
        when(billingService.createTransaction(any(), any(), any(), any())).thenReturn(new BillingTransaction());
        when(billingMapper.toBillingTransactionDto(any())).thenReturn(
            new BillingTransactionDto("TX", BigDecimal.ONE, "desc", OffsetDateTime.now())
        );
        when(notificationService.createNotification(any(), any(), any(), anyBoolean())).thenReturn(notification);
        when(notificationMapper.toNotificationDto(any())).thenReturn(
            new NotificationDto("TARIFF_CHANGED", "ok", true, OffsetDateTime.now())
        );

        ChangeTariffResponse response = service.changeTariff(1L, new ChangeTariffRequest(2L, Map.of()));

        verify(transactionTemplate).execute(any());
        ArgumentCaptor<Subscriber> subscriberCaptor = ArgumentCaptor.forClass(Subscriber.class);
        verify(subscriberService).save(subscriberCaptor.capture());
        assertThat(subscriberCaptor.getValue().getCurrentTariff().getId()).isEqualTo(2L);
        assertThat(subscriberCaptor.getValue().getBalance()).isEqualByComparingTo("85");
        assertThat(response.success()).isTrue();
        assertThat(response.billingTransactions()).hasSize(2);
    }
}
