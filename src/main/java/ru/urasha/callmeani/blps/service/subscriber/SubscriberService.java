package ru.urasha.callmeani.blps.service.subscriber;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;

import java.util.List;

public interface SubscriberService {
    Page<SubscriberResponse> getSubscribers(Pageable pageable);
    SubscriberResponse getSubscriber(Long id);
    SubscriberResponse createSubscriber(SubscriberUpsertRequest request);
    SubscriberResponse updateSubscriber(Long id, SubscriberUpsertRequest request);
    void deleteSubscriber(Long id);
    Subscriber getSubscriberEntity(Long id);
    Subscriber save(Subscriber subscriber);
    List<Subscriber> findWithCurrentTariff();
}
