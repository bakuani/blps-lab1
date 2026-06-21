package ru.urasha.callmeani.blps.service.notification.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.enums.NotificationType;
import ru.urasha.callmeani.blps.repository.NotificationEventRepository;
import ru.urasha.callmeani.blps.service.notification.NotificationService;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationEventRepository notificationEventRepository;

    @Override
    @Transactional
    public NotificationEvent createNotification(Subscriber subscriber, NotificationType type, String message, boolean success) {
        NotificationEvent notification = new NotificationEvent();
        notification.setSubscriber(subscriber);
        notification.setType(type);
        notification.setMessage(message);
        notification.setSuccess(success);
        notification.setCreatedAt(OffsetDateTime.now());
        
        NotificationEvent saved = notificationEventRepository.save(notification);
        log.info(
            "Notification created: notificationId={}, subscriberId={}, type={}, success={}",
            saved.getId(),
            subscriber.getId(),
            type,
            success
        );
        return saved;
    }
}



