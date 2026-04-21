package ru.urasha.callmeani.blps.service.billing.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;
import ru.urasha.callmeani.blps.repository.BillingTransactionRepository;
import ru.urasha.callmeani.blps.service.billing.BillingService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        
        return billingTransactionRepository.save(transaction);
    }
}



