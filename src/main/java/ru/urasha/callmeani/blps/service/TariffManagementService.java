package ru.urasha.callmeani.blps.service;

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
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.repository.NotificationEventRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.TariffCategoryRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
public class TariffManagementService {

    private final SubscriberRepository subscriberRepository;
    private final TariffRepository tariffRepository;
    private final TariffCategoryRepository tariffCategoryRepository;
    private final BillingTransactionRepository billingTransactionRepository;
    private final NotificationEventRepository notificationEventRepository;

    public TariffManagementService(
        SubscriberRepository subscriberRepository,
        TariffRepository tariffRepository,
        TariffCategoryRepository tariffCategoryRepository,
        BillingTransactionRepository billingTransactionRepository,
        NotificationEventRepository notificationEventRepository
    ) {
        this.subscriberRepository = subscriberRepository;
        this.tariffRepository = tariffRepository;
        this.tariffCategoryRepository = tariffCategoryRepository;
        this.billingTransactionRepository = billingTransactionRepository;
        this.notificationEventRepository = notificationEventRepository;
    }

    @Transactional(readOnly = true)
    public TariffInfoResponse getSubscriberTariffInfo(Long subscriberId) {
        Subscriber subscriber = getSubscriber(subscriberId);
        TariffSummaryDto currentTariff = toTariffSummary(subscriber.getCurrentTariff());
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
            .map(c -> new IdNameDto(c.getId(), c.getName()))
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
            .map(this::toTariffSummary)
            .toList();
    }

    @Transactional(readOnly = true)
    public TariffDetailsResponse getTariffDetails(Long tariffId) {
        Tariff tariff = tariffRepository.findById(tariffId)
            .orElseThrow(() -> new NotFoundException("Tariff not found: " + tariffId));

        List<TariffOptionDto> options = tariff.getOptions()
            .stream()
            .map(option -> new TariffOptionDto(
                option.getId(),
                option.getName(),
                option.getDescription(),
                option.getPrice()
            ))
            .toList();

        return new TariffDetailsResponse(
            tariff.getId(),
            tariff.getName(),
            tariff.getDescription(),
            tariff.getCategory().getName(),
            tariff.getMonthlyFee(),
            tariff.getSwitchFee(),
            tariff.isCustomizable(),
            tariff.getPdfUrl(),
            options
        );
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
                toTariffSummary(currentTariff),
                toTariffSummary(currentTariff),
                safeOptions(request.options()),
                subscriber.getBalance(),
                List.of(),
                toNotificationDto(notification)
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
                toTariffSummary(currentTariff),
                toTariffSummary(targetTariff),
                safeOptions(request.options()),
                subscriber.getBalance(),
                List.of(),
                toNotificationDto(notification)
            );
        }

        List<BillingTransactionDto> transactions = new ArrayList<>();
        if (switchFee.compareTo(BigDecimal.ZERO) > 0) {
            transactions.add(toBillingDto(charge(subscriber, BillingTransactionType.TARIFF_SWITCH_FEE, switchFee,
                "Списание платы за смену тарифа")));
        }

        transactions.add(toBillingDto(charge(subscriber, BillingTransactionType.MONTHLY_TARIFF_FEE, monthlyFee,
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
            toTariffSummary(currentTariff),
            toTariffSummary(targetTariff),
            safeOptions(request.options()),
            subscriber.getBalance(),
            transactions,
            toNotificationDto(notification)
        );
    }

    private Subscriber getSubscriber(Long subscriberId) {
        return subscriberRepository.findById(subscriberId)
            .orElseThrow(() -> new NotFoundException("Subscriber not found: " + subscriberId));
    }

    private BillingTransaction charge(Subscriber subscriber, BillingTransactionType type, BigDecimal amount, String description) {
        subscriber.setBalance(subscriber.getBalance().subtract(amount));

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

    private TariffSummaryDto toTariffSummary(Tariff tariff) {
        if (tariff == null) {
            return null;
        }
        TariffCategory category = tariff.getCategory();
        String categoryName = category == null ? null : category.getName();
        return new TariffSummaryDto(
            tariff.getId(),
            tariff.getName(),
            categoryName,
            tariff.getMonthlyFee()
        );
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

    private Map<String, String> safeOptions(Map<String, String> options) {
        return options == null ? Collections.emptyMap() : options;
    }
}
