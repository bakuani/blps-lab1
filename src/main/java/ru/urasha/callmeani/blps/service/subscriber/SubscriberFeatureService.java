package ru.urasha.callmeani.blps.service.subscriber;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureUpsertRequest;

public interface SubscriberFeatureService {
    Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable);
    SubscriberFeatureResponse getSubscriberFeature(Long id);
    SubscriberFeatureResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request);
    SubscriberFeatureResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request);
    void deleteSubscriberFeature(Long id);
    ru.urasha.callmeani.blps.domain.entity.SubscriberFeature getSubscriberFeatureEntity(Long id);
    java.util.List<ru.urasha.callmeani.blps.domain.entity.SubscriberFeature> findBySubscriberIdAndStatus(Long subscriberId, ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus status);
    java.util.Optional<ru.urasha.callmeani.blps.domain.entity.SubscriberFeature> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long featureId, ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus status);
    void delete(ru.urasha.callmeani.blps.domain.entity.SubscriberFeature feature);
}






