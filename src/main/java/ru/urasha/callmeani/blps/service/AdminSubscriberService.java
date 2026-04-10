package ru.urasha.callmeani.blps.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberUpsertRequest;

public interface AdminSubscriberService {
    Page<SubscriberAdminResponse> getSubscribers(Pageable pageable);
    SubscriberAdminResponse getSubscriber(Long id);
    SubscriberAdminResponse createSubscriber(SubscriberUpsertRequest request);
    SubscriberAdminResponse updateSubscriber(Long id, SubscriberUpsertRequest request);
    void deleteSubscriber(Long id);
}