package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberFeatureUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.AdditionalFeature;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.SubscriberFeature;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.AdditionalFeatureRepository;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.SubscriberFeatureRepository;
import ru.urasha.callmeani.blps.service.AdminSubscriberFeatureService;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class AdminSubscriberFeatureServiceImpl implements AdminSubscriberFeatureService {

    private final SubscriberFeatureRepository SubscriberFeatureRepository;
    private final SubscriberRepository subscriberRepository;
    private final AdditionalFeatureRepository AdditionalFeatureRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public Page<SubscriberFeatureAdminResponse> getSubscriberFeatures(Pageable pageable) {
        return SubscriberFeatureRepository.findAll(pageable).map(adminMapper::toSubscriberFeatureResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberFeatureAdminResponse getSubscriberFeature(Long id) {
        return adminMapper.toSubscriberFeatureResponse(getSubscriberFeatureEntity(id));
    }

    @Transactional
    public SubscriberFeatureAdminResponse createSubscriberFeature(SubscriberFeatureUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature service = getServiceEntity(request.serviceId());
        SubscriberFeature SubscriberFeature = new SubscriberFeature();
        adminMapper.updateSubscriberFeature(SubscriberFeature, request);
        SubscriberFeature.setSubscriber(subscriber);
        SubscriberFeature.setService(service);
        if (SubscriberFeature.getConnectedAt() == null) {
            SubscriberFeature.setConnectedAt(OffsetDateTime.now());
        }
        return adminMapper.toSubscriberFeatureResponse(SubscriberFeatureRepository.save(SubscriberFeature));
    }

    @Transactional
    public SubscriberFeatureAdminResponse updateSubscriberFeature(Long id, SubscriberFeatureUpsertRequest request) {
        SubscriberFeature SubscriberFeature = getSubscriberFeatureEntity(id);
        Subscriber subscriber = getSubscriberEntity(request.subscriberId());
        AdditionalFeature service = getServiceEntity(request.serviceId());
        adminMapper.updateSubscriberFeature(SubscriberFeature, request);
        SubscriberFeature.setSubscriber(subscriber);
        SubscriberFeature.setService(service);
        return adminMapper.toSubscriberFeatureResponse(SubscriberFeatureRepository.save(SubscriberFeature));
    }

    @Transactional
    public void deleteSubscriberFeature(Long id) {
        SubscriberFeatureRepository.delete(getSubscriberFeatureEntity(id));
    }

    private SubscriberFeature getSubscriberFeatureEntity(Long id) {
        return SubscriberFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber service not found: " + id));
    }

    private Subscriber getSubscriberEntity(Long id) {
        return subscriberRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber not found: " + id));
    }

    private AdditionalFeature getServiceEntity(Long id) {
        return AdditionalFeatureRepository.findById(id).orElseThrow(() -> new NotFoundException("Service not found: " + id));
    }
}
