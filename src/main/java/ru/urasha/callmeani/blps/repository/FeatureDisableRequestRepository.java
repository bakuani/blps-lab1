package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.enums.TariffChangeRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface FeatureDisableRequestRepository extends JpaRepository<FeatureDisableRequest, Long> {
    List<FeatureDisableRequest> findByStatusInAndUpdatedAtBefore(List<TariffChangeRequestStatus> statuses, OffsetDateTime threshold);
}
