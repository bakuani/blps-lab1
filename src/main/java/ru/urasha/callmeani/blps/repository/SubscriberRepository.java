package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;

import java.util.List;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
    List<Subscriber> findByCurrentTariffIsNotNull();
}
