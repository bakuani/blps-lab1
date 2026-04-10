package ru.urasha.callmeani.blps.service;

import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.BillingTransactionType;

import java.math.BigDecimal;

public interface BillingService {
    BillingTransaction createTransaction(Subscriber subscriber, BillingTransactionType type, BigDecimal amount, String description);
}