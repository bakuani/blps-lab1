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

import ru.urasha.callmeani.blps.service.feature.AdditionalFeatureService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;
import ru.urasha.callmeani.blps.repository.SubscriberFeatureRepository;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberFeatureService;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class SubscriberFeatureServiceImpl implements SubscriberFeatureService {

    private final SubscriberFeatureRepository SubscriberFeatureRepository;
    private final SubscriberService subscriberService;
    private final AdditionalFeatureService additionalFeatureService;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public Page<SubscriberFeatureResponse> getSubscriberFeatures(Pageable pageable) {
        return SubscriberFeatureRepository.findAll(pageable).map(subscriberMapper::toSubscriberFeatureResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberFeatureResponse getSubscriberFeature(Long id) {
        return subscriberMapper.toSubscriberFeatureResponse(getSubscriberFeatureEntity(id));
    }

    @Transactional
    public SubscriberFeatureResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature feature = getFeatureEntity(request.featureId());
        SubscriberFeature SubscriberFeature = new SubscriberFeature();
        subscriberMapper.updateSubscriberFeature(SubscriberFeature, request);
        SubscriberFeature.setSubscriber(subscriber);
        SubscriberFeature.setService(feature);
        if (SubscriberFeature.getConnectedAt() == null) {
            SubscriberFeature.setConnectedAt(OffsetDateTime.now());
        }
        return subscriberMapper.toSubscriberFeatureResponse(SubscriberFeatureRepository.save(SubscriberFeature));
    }

    @Transactional
    public SubscriberFeatureResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request) {
        SubscriberFeature SubscriberFeature = getSubscriberFeatureEntity(id);
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature feature = getFeatureEntity(request.featureId());
        subscriberMapper.updateSubscriberFeature(SubscriberFeature, request);
        SubscriberFeature.setSubscriber(subscriber);
        SubscriberFeature.setService(feature);
        return subscriberMapper.toSubscriberFeatureResponse(SubscriberFeatureRepository.save(SubscriberFeature));
    }

    @Transactional
    public void deleteSubscriberFeature(Long id) {
        SubscriberFeatureRepository.delete(getSubscriberFeatureEntity(id));
    }

    public SubscriberFeature getSubscriberFeatureEntity(Long id) {
        return SubscriberFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber feature not found: " + id));
    }

    private Subscriber getSubscriberEntity(Long id) {
        return subscriberService.getSubscriberEntity(id);
    }

    private AdditionalFeature getFeatureEntity(Long id) {
        return additionalFeatureService.getAdditionalFeatureEntity(id);
    }

    public java.util.List<ru.urasha.callmeani.blps.domain.entity.SubscriberFeature> findBySubscriberIdAndStatus(Long subscriberId, ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus status) {
        return SubscriberFeatureRepository.findBySubscriberIdAndStatus(subscriberId, status);
    }

    public java.util.Optional<ru.urasha.callmeani.blps.domain.entity.SubscriberFeature> findBySubscriberIdAndServiceIdAndStatus(Long subscriberId, Long featureId, ru.urasha.callmeani.blps.domain.enums.SubscriberFeatureStatus status) {
        return SubscriberFeatureRepository.findBySubscriberIdAndServiceIdAndStatus(subscriberId, featureId, status);
    }

    public void delete(ru.urasha.callmeani.blps.domain.entity.SubscriberFeature feature) {
        SubscriberFeatureRepository.delete(feature);
    }
}








