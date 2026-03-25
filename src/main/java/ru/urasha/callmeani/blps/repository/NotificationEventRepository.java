package ru.urasha.callmeani.blps.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;

public interface NotificationEventRepository extends JpaRepository<NotificationEvent, Long> {
}
