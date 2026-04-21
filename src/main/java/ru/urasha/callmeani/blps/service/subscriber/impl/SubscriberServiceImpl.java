package ru.urasha.callmeani.blps.service.subscriber.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;

import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

@Service
@RequiredArgsConstructor
// TODO: remove admin
public class SubscriberServiceImpl implements SubscriberService {

    // TODO: 1 сервис = 1 репозиторий
    private final SubscriberRepository subscriberRepository;
    private final TariffService tariffService;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public Page<SubscriberResponse> getSubscribers(Pageable pageable) {
        return subscriberRepository.findAll(pageable).map(subscriberMapper::toSubscriberResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberResponse getSubscriber(Long id) {
        return subscriberMapper.toSubscriberResponse(getSubscriberEntity(id));
    }

    @Transactional
    public SubscriberResponse createSubscriber(SubscriberUpsertRequest request) {
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        Subscriber subscriber = new Subscriber();
        subscriberMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        return subscriberMapper.toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public SubscriberResponse updateSubscriber(Long id, SubscriberUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(id);
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        subscriberMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        return subscriberMapper.toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        subscriberRepository.delete(getSubscriberEntity(id));
    }

    public Subscriber getSubscriberEntity(Long id) {
        return subscriberRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber not found: " + id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffService.getTariffEntity(id);
    }

    public ru.urasha.callmeani.blps.domain.entity.Subscriber save(ru.urasha.callmeani.blps.domain.entity.Subscriber subscriber) {
        return subscriberRepository.save(subscriber);
    }
}








