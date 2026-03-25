package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;

public interface SubscriberRepository extends JpaRepository<Subscriber, Long> {
}
