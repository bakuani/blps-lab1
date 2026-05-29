package ru.urasha.callmeani.blps.service.subscriber.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberResponse;
import ru.urasha.callmeani.blps.api.dto.subscriber.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.SubscriberNotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.mapper.SubscriberMapper;

import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.service.eis.impl.DolibarrSubscriberSyncService;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.subscriber.SubscriberService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriberServiceImpl implements SubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final TariffService tariffService;
    private final SubscriberMapper subscriberMapper;
    private final DolibarrSubscriberSyncService dolibarrSubscriberSyncService;

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
        Subscriber saved = subscriberRepository.save(subscriber);
        dolibarrSubscriberSyncService.syncSubscriber(saved);
        return subscriberMapper.toSubscriberResponse(saved);
    }

    @Transactional
    public SubscriberResponse updateSubscriber(Long id, SubscriberUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(id);
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        subscriberMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        Subscriber saved = subscriberRepository.save(subscriber);
        dolibarrSubscriberSyncService.syncSubscriber(saved);
        return subscriberMapper.toSubscriberResponse(saved);
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        subscriberRepository.delete(getSubscriberEntity(id));
    }

    public Subscriber getSubscriberEntity(Long id) {
        return subscriberRepository.findById(id).orElseThrow(() -> new SubscriberNotFoundException(id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffService.getTariffEntity(id);
    }

    public Subscriber save(Subscriber subscriber) {
        return subscriberRepository.save(subscriber);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Subscriber> findWithCurrentTariff() {
        return subscriberRepository.findByCurrentTariffIsNotNull();
    }
}
