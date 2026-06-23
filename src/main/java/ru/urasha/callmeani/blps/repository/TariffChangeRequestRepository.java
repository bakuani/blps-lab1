package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.domain.enums.BusinessRequestStatus;

import java.time.OffsetDateTime;
import java.util.List;

public interface TariffChangeRequestRepository extends JpaRepository<TariffChangeRequest, Long> {
    List<TariffChangeRequest> findByStatusInAndUpdatedAtBefore(List<BusinessRequestStatus> statuses, OffsetDateTime threshold);
}
