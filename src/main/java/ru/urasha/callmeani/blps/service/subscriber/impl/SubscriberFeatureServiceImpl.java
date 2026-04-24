package ru.urasha.callmeani.blps.service.subscriber.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus;
import ru.urasha.callmeani.blps.mapper.SubscriberMapper;

import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.repository.SubscriberFeatureRepository;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriberFeatureServiceImpl implements SubscriberFeatureService {

    private final SubscriberFeatureRepository subscriberFeatureRepository;
    private final SubscriberService subscriberService;
    private final AdditionalFeatureService additionalFeatureService;
    private final SubscriberMapper subscriberMapper;

    @Transactional(readOnly = true)
    public Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable) {
        return subscriberFeatureRepository.findAll(pageable).map(subscriberMapper::toSubscriberFeatureResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberFeatureResponse getSubscriberFeature(Long id) {
        return subscriberMapper.toSubscriberFeatureResponse(getSubscriberFeatureEntity(id));
    }

    @Transactional
    public SubscriberFeatureResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature feature = getFeatureEntity(request.featureId());
        SubscriberFeature subscriberFeature = new SubscriberFeature();
        subscriberMapper.updateSubscriberFeature(subscriberFeature, request);
        subscriberFeature.setSubscriber(subscriber);
        subscriberFeature.setService(feature);
        if (subscriberFeature.getConnectedAt() == null) {
            subscriberFeature.setConnectedAt(OffsetDateTime.now());
        }
        return subscriberMapper.toSubscriberFeatureResponse(subscriberFeatureRepository.save(subscriberFeature));
    }

    @Transactional
    public SubscriberFeatureResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request) {
        SubscriberFeature subscriberFeature = getSubscriberFeatureEntity(id);
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature feature = getFeatureEntity(request.featureId());
        subscriberMapper.updateSubscriberFeature(subscriberFeature, request);
        subscriberFeature.setSubscriber(subscriber);
        subscriberFeature.setService(feature);
        return subscriberMapper.toSubscriberFeatureResponse(subscriberFeatureRepository.save(subscriberFeature));
    }

    @Transactional
    public void deleteSubscriberFeature(Long id) {
        subscriberFeatureRepository.delete(getSubscriberFeatureEntity(id));
    }

    public SubscriberFeature getSubscriberFeatureEntity(Long id) {
        return subscriberFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber feature not found: " + id));
    }

    private Subscriber getSubscriberEntity(Long id) {
        return subscriberService.getSubscriberEntity(id);
    }

    private AdditionalFeature getFeatureEntity(Long id) {
        return additionalFeatureService.getAdditionalFeatureEntity(id);
    }

    public List<SubscriberFeature> findBySubscriberIdAndStatus(Long subscriberId, SubscriberFeatureStatus status) {
        return subscriberFeatureRepository.findBySubscriberIdAndStatus(subscriberId, status);
    }

    public Optional<SubscriberFeature> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long featureId, SubscriberFeatureStatus status) {
        return subscriberFeatureRepository.findBySubscriberIdAndServiceIdAndStatus(subscriberId, featureId, status);
    }

    public void delete(SubscriberFeature feature) {
        subscriberFeatureRepository.delete(feature);
    }
}
