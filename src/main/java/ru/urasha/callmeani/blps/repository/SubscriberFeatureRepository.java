package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriberFeatureRepository extends JpaRepository<SubscriberFeature, Long> {

    List<SubscriberFeature> findBySubscriberIdAndStatus(Long subscriberId, SubscriberFeatureStatus status);

    Optional<SubscriberFeature> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long serviceId, SubscriberFeatureStatus status);
}

