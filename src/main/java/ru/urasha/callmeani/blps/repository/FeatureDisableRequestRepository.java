package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;

public interface FeatureDisableRequestRepository extends JpaRepository<FeatureDisableRequest, Long> {
}
