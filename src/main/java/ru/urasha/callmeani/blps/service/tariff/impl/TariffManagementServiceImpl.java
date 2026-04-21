package ru.urasha.callmeani.blps.service.tariff.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.billing.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;


import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.tariff.TariffCategoryService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.urasha.callmeani.blps.service.tariff.TariffManagementService;
import ru.urasha.callmeani.blps.service.billing.BillingService;
import ru.urasha.callmeani.blps.service.notification.NotificationService;

@Service
@RequiredArgsConstructor
public class TariffManagementServiceImpl implements TariffManagementService {

    private final SubscriberService subscriberService;
    private final TariffService tariffService;
    private final TariffCategoryService tariffCategoryService;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;
    private final ru.urasha.callmeani.blps.mapper.BillingMapper billingMapper;
    private final ru.urasha.callmeani.blps.mapper.NotificationMapper notificationMapper;

    @Transactional(readOnly = true)
    public TariffInfoResponse getSubscriberTariffInfo(Long subscriberId) {
        Subscriber subscriber = getSubscriber(subscriberId);
        TariffSummaryDto currentTariff = subscriber.getCurrentTariff() != null ? tariffMapper.toTariffSummaryDto(subscriber.getCurrentTariff()) : null;
        return new TariffInfoResponse(
                subscriber.getId(),
                subscriber.getPhone(),
                subscriber.getFullName(),
                subscriber.getBalance(),
                currentTariff
        );
    }

    @Transactional(readOnly = true)
    public List<IdNameDto> getTariffCategories() {
        return tariffCategoryService.findAll()
                .stream()
                .map(tariffMapper::toIdNameDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TariffSummaryDto> findTariffs(Long categoryId, String query) {
        List<Tariff> tariffs;
        boolean hasCategory = categoryId != null;
        boolean hasQuery = query != null && !query.isBlank();

        if (hasCategory && hasQuery) {
            tariffs = tariffService.findByCategoryIdAndNameContainingIgnoreCase(categoryId, query.trim());
        } else if (hasCategory) {
            tariffs = tariffService.findByCategoryId(categoryId);
        } else if (hasQuery) {
            tariffs = tariffService.findByNameContainingIgnoreCase(query.trim());
        } else {
            tariffs = tariffService.findAll();
        }

        return tariffs.stream()
                .map(tariffMapper::toTariffSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TariffDetailsResponse getTariffDetails(Long tariffId) {
        Tariff tariff = java.util.Optional.of(tariffService.getTariffEntity(tariffId))
                .orElseThrow(() -> new NotFoundException("Tariff not found: " + tariffId));

        return tariffMapper.toTariffDetailsResponse(tariff);
    }

    @Transactional
    public ChangeTariffResponse changeTariff(Long subscriberId, ChangeTariffRequest request) {
        Subscriber subscriber = getSubscriber(subscriberId);
        Tariff targetTariff = java.util.Optional.of(tariffService.getTariffEntity(request.targetTariffId()))
                .orElseThrow(() -> new NotFoundException("Tariff not found: " + request.targetTariffId()));

        Tariff currentTariff = subscriber.getCurrentTariff();
        if (currentTariff != null && Objects.equals(currentTariff.getId(), targetTariff.getId())) {
            NotificationEvent notification = saveNotification(
                    subscriber,
                    NotificationType.TARIFF_CHANGE_ERROR,
                    "Выбран текущий тариф. Смена не выполнена.",
                    false
            );
            return new ChangeTariffResponse(
                    false,
                    "Current tariff is already selected",
                    tariffMapper.toTariffSummaryDto(currentTariff),
                    tariffMapper.toTariffSummaryDto(currentTariff),
                    safeOptions(request.options()),
                    subscriber.getBalance(),
                    List.of(),
                    notificationMapper.toNotificationDto(notification)
            );
        }

        BigDecimal switchFee = targetTariff.getSwitchFee();
        BigDecimal monthlyFee = targetTariff.getMonthlyFee();
        BigDecimal total = switchFee.add(monthlyFee);

        if (subscriber.getBalance().compareTo(total) < 0) {
            NotificationEvent notification = saveNotification(
                    subscriber,
                    NotificationType.TARIFF_CHANGE_ERROR,
                    "Недостаточно средств для смены тарифа.",
                    false
            );
            return new ChangeTariffResponse(
                    false,
                    "Insufficient funds",
                    currentTariff != null ? tariffMapper.toTariffSummaryDto(currentTariff) : null,
                    tariffMapper.toTariffSummaryDto(targetTariff),
                    safeOptions(request.options()),
                    subscriber.getBalance(),
                    List.of(),
                    notificationMapper.toNotificationDto(notification)
            );
        }

        List<BillingTransactionDto> transactions = new ArrayList<>();
        if (switchFee.compareTo(BigDecimal.ZERO) > 0) {
            transactions.add(billingMapper.toBillingTransactionDto(charge(subscriber, BillingTransactionType.TARIFF_SWITCH_FEE, switchFee,
                    "Списание платы за смену тарифа")));
        }

        transactions.add(billingMapper.toBillingTransactionDto(charge(subscriber, BillingTransactionType.MONTHLY_TARIFF_FEE, monthlyFee,
                "Списание абонентской платы нового тарифа")));

        subscriber.setCurrentTariff(targetTariff);
        subscriberService.save(subscriber);

        NotificationEvent notification = saveNotification(
                subscriber,
                NotificationType.TARIFF_CHANGED,
                "Тариф успешно изменен.",
                true
        );

        return new ChangeTariffResponse(
                true,
                "Tariff changed successfully",
                currentTariff != null ? tariffMapper.toTariffSummaryDto(currentTariff) : null,
                tariffMapper.toTariffSummaryDto(targetTariff),
                safeOptions(request.options()),
                subscriber.getBalance(),
                transactions,
                notificationMapper.toNotificationDto(notification)
        );
    }

    private Subscriber getSubscriber(Long subscriberId) {
        return java.util.Optional.of(subscriberService.getSubscriberEntity(subscriberId))
                .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));
    }

    private BillingTransaction charge(Subscriber subscriber, BillingTransactionType type, BigDecimal amount, String description) {
        subscriber.setBalance(subscriber.getBalance().subtract(amount));
        return billingService.createTransaction(subscriber, type, amount, description);
    }

    private NotificationEvent saveNotification(Subscriber subscriber, NotificationType type, String message, boolean success) {
        return notificationService.createNotification(subscriber, type, message, success);
    }

    private Map<String, String> safeOptions(Map<String, String> options) {
        return options == null ? Collections.emptyMap() : options;
    }
}






