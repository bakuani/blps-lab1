package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.MonthlyFeeChargeRequest;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface MonthlyFeeChargeRequestRepository extends JpaRepository<MonthlyFeeChargeRequest, Long> {
    boolean existsBySubscriberIdAndBillingPeriod(Long subscriberId, String billingPeriod);
    List<MonthlyFeeChargeRequest> findTop10BySubscriberIdOrderByCreatedAtDesc(Long subscriberId);
    List<MonthlyFeeChargeRequest> findByStatusInAndUpdatedAtBefore(List<BusinessRequestStatus> statuses, OffsetDateTime threshold);
}
