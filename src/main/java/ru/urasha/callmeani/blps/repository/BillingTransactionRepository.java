package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.BillingTransaction;

public interface BillingTransactionRepository extends JpaRepository<BillingTransaction, Long> {
}
