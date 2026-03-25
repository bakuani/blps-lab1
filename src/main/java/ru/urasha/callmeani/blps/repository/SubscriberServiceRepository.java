package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.SubscriberService;
import ru.urasha.callmeani.blps.domain.enums.SubscriberServiceStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriberServiceRepository extends JpaRepository<SubscriberService, Long> {

    List<SubscriberService> findBySubscriberIdAndStatus(Long subscriberId, SubscriberServiceStatus status);

    Optional<SubscriberService> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long serviceId, SubscriberServiceStatus status);
}
