package ru.urasha.callmeani.blps.service.subscriber;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;

import java.util.List;
import java.util.Optional;

public interface SubscriberFeatureService {
    Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable);
    SubscriberFeatureResponse getSubscriberFeature(Long id);
    SubscriberFeatureResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request);
    SubscriberFeatureResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request);
    void deleteSubscriberFeature(Long id);
    SubscriberFeature getSubscriberFeatureEntity(Long id);
    List<SubscriberFeature> findBySubscriberIdAndStatus(Long subscriberId, SubscriberFeatureStatus status);
    Optional<SubscriberFeature> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long featureId, SubscriberFeatureStatus status);
    void delete(SubscriberFeature feature);
    SubscriberFeature save(SubscriberFeature feature);
}
