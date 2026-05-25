package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;

public interface TariffChangeRequestRepository extends JpaRepository<TariffChangeRequest, Long> {
}
