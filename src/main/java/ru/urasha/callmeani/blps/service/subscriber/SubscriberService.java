package ru.urasha.callmeani.blps.service.subscriber;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;

public interface SubscriberService {
    Page<SubscriberResponse> getSubscribers(Pageable pageable);
    SubscriberResponse getSubscriber(Long id);
    SubscriberResponse createSubscriber(SubscriberUpsertRequest request);
    SubscriberResponse updateSubscriber(Long id, SubscriberUpsertRequest request);
    void deleteSubscriber(Long id);
    ru.urasha.callmeani.blps.domain.entity.Subscriber getSubscriberEntity(Long id);
    ru.urasha.callmeani.blps.domain.entity.Subscriber save(ru.urasha.callmeani.blps.domain.entity.Subscriber subscriber);
}





