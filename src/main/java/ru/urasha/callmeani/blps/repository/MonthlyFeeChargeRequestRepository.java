package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;

import java.util.List;

public interface MonthlyFeeChargeRequestRepository extends JpaRepository<MonthlyFeeChargeRequest, Long> {
    boolean existsBySubscriberIdAndBillingPeriod(Long subscriberId, String billingPeriod);
    List<MonthlyFeeChargeRequest> findTop10BySubscriberIdOrderByCreatedAtDesc(Long subscriberId);
}
