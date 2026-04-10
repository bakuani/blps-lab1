package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.BillingTransactionDto;
import ru.urasha.callmeani.blps.api.dto.ChangeTariffRequest;
import ru.urasha.callmeani.blps.api.dto.ChangeTariffResponse;
import ru.urasha.callmeani.blps.api.dto.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.NotificationDto;
import ru.urasha.callmeani.blps.api.dto.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.TariffInfoResponse;
import ru.urasha.callmeani.blps.api.dto.TariffOptionDto;
import ru.urasha.callmeani.blps.api.dto.TariffSummaryDto;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;

import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.mapper.ClientMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import ru.urasha.callmeani.blps.service.TariffManagementService;
import ru.urasha.callmeani.blps.service.BillingService;
import ru.urasha.callmeani.blps.service.NotificationService;

@Service
@RequiredArgsConstructor
public class TariffManagementServiceImpl implements TariffManagementService {

    private final SubscriberRepository subscriberRepository;
    private final TariffRepository tariffRepository;
    private final TariffCategoryRepository tariffCategoryRepository;
    private final BillingService billingService;
    private final NotificationService notificationService;
    private final ClientMapper clientMapper;

    @Transactional(readOnly = true)
    public TariffInfoResponse getSubscriberTariffInfo(Long subscriberId) {
        Subscriber subscriber = getSubscriber(subscriberId);
        TariffSummaryDto currentTariff = subscriber.getCurrentTariff() != null ? clientMapper.toTariffSummaryDto(subscriber.getCurrentTariff()) : null;
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
        return tariffCategoryRepository.findAll()
                .stream()
                .map(clientMapper::toIdNameDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TariffSummaryDto> findTariffs(Long categoryId, String query) {
        List<Tariff> tariffs;
        boolean hasCategory = categoryId != null;
        boolean hasQuery = query != null && !query.isBlank();

        if (hasCategory && hasQuery) {
            tariffs = tariffRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, query.trim());
        } else if (hasCategory) {
            tariffs = tariffRepository.findByCategoryId(categoryId);
        } else if (hasQuery) {
            tariffs = tariffRepository.findByNameContainingIgnoreCase(query.trim());
        } else {
            tariffs = tariffRepository.findAll();
        }

        return tariffs.stream()
                .map(clientMapper::toTariffSummaryDto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TariffDetailsResponse getTariffDetails(Long tariffId) {
        Tariff tariff = tariffRepository.findById(tariffId)
                .orElseThrow(() -> new NotFoundException("Tariff not found: " + tariffId));

        return clientMapper.toTariffDetailsResponse(tariff);
    }

    @Transactional
    public ChangeTariffResponse changeTariff(Long subscriberId, ChangeTariffRequest request) {
        Subscriber subscriber = getSubscriber(subscriberId);
        Tariff targetTariff = tariffRepository.findById(request.targetTariffId())
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
                    clientMapper.toTariffSummaryDto(currentTariff),
                    clientMapper.toTariffSummaryDto(currentTariff),
                    safeOptions(request.options()),
                    subscriber.getBalance(),
                    List.of(),
                    clientMapper.toNotificationDto(notification)
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
                    currentTariff != null ? clientMapper.toTariffSummaryDto(currentTariff) : null,
                    clientMapper.toTariffSummaryDto(targetTariff),
                    safeOptions(request.options()),
                    subscriber.getBalance(),
                    List.of(),
                    clientMapper.toNotificationDto(notification)
            );
        }

        List<BillingTransactionDto> transactions = new ArrayList<>();
        if (switchFee.compareTo(BigDecimal.ZERO) > 0) {
            transactions.add(clientMapper.toBillingTransactionDto(charge(subscriber, BillingTransactionType.TARIFF_SWITCH_FEE, switchFee,
                    "Списание платы за смену тарифа")));
        }

        transactions.add(clientMapper.toBillingTransactionDto(charge(subscriber, BillingTransactionType.MONTHLY_TARIFF_FEE, monthlyFee,
                "Списание абонентской платы нового тарифа")));

        subscriber.setCurrentTariff(targetTariff);
        subscriberRepository.save(subscriber);

        NotificationEvent notification = saveNotification(
                subscriber,
                NotificationType.TARIFF_CHANGED,
                "Тариф успешно изменен.",
                true
        );

        return new ChangeTariffResponse(
                true,
                "Tariff changed successfully",
                currentTariff != null ? clientMapper.toTariffSummaryDto(currentTariff) : null,
                clientMapper.toTariffSummaryDto(targetTariff),
                safeOptions(request.options()),
                subscriber.getBalance(),
                transactions,
                clientMapper.toNotificationDto(notification)
        );
    }

    private Subscriber getSubscriber(Long subscriberId) {
        return subscriberRepository.findById(subscriberId)
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
