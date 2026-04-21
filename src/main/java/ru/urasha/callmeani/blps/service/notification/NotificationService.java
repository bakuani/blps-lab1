package ru.urasha.callmeani.blps.service.notification;

import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;

public interface NotificationService {
    NotificationEvent createNotification(Subscriber subscriber, NotificationType type, String message, boolean success);
}


