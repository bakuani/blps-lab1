package ru.urasha.callmeani.blps.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureUpsertRequest;

public interface AdminSubscriberFeatureService {
    Page<SubscriberFeatureAdminResponse> getSubscriberFeatures(Pageable pageable);
    SubscriberFeatureAdminResponse getSubscriberFeature(Long id);
    SubscriberFeatureAdminResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request);
    SubscriberFeatureAdminResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request);
    void deleteSubscriberFeature(Long id);
}
