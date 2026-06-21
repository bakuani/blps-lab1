package ru.urasha.callmeani.blps.service.billing.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class BillingServiceImpl implements BillingService {

    private final BillingTransactionRepository billingTransactionRepository;

    @Override
    @Transactional
    public BillingTransaction createTransaction(Subscriber subscriber, BillingTransactionType type, BigDecimal amount, String description) {
        BillingTransaction transaction = new BillingTransaction();
        transaction.setSubscriber(subscriber);
        transaction.setType(type);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setCreatedAt(OffsetDateTime.now());
        
        BillingTransaction saved = billingTransactionRepository.save(transaction);
        log.info(
            "Billing transaction created: transactionId={}, subscriberId={}, type={}, amount={}",
            saved.getId(),
            subscriber.getId(),
            type,
            amount
        );
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsBySubscriberIdAndTypeAndDescription(Long subscriberId, BillingTransactionType type, String description) {
        return billingTransactionRepository.existsBySubscriberIdAndTypeAndDescription(subscriberId, type, description);
    }
}



