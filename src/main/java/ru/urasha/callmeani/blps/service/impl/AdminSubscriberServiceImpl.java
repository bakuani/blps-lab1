package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.SubscriberUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Subscriber;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.SubscriberRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.service.AdminSubscriberService;

@Service
@RequiredArgsConstructor
public class AdminSubscriberServiceImpl implements AdminSubscriberService {

    private final SubscriberRepository subscriberRepository;
    private final TariffRepository tariffRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public Page<SubscriberAdminResponse> getSubscribers(Pageable pageable) {
        return subscriberRepository.findAll(pageable).map(adminMapper::toSubscriberResponse);
    }

    @Transactional(readOnly = true)
    public SubscriberAdminResponse getSubscriber(Long id) {
        return adminMapper.toSubscriberResponse(getSubscriberEntity(id));
    }

    @Transactional
    public SubscriberAdminResponse createSubscriber(SubscriberUpsertRequest request) {
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        Subscriber subscriber = new Subscriber();
        adminMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        return adminMapper.toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public SubscriberAdminResponse updateSubscriber(Long id, SubscriberUpsertRequest request) {
        Subscriber subscriber = getSubscriberEntity(id);
        Tariff tariff = request.currentTariffId() == null ? null : getTariffEntity(request.currentTariffId());
        adminMapper.updateSubscriber(subscriber, request);
        subscriber.setCurrentTariff(tariff);
        return adminMapper.toSubscriberResponse(subscriberRepository.save(subscriber));
    }

    @Transactional
    public void deleteSubscriber(Long id) {
        subscriberRepository.delete(getSubscriberEntity(id));
    }

    private Subscriber getSubscriberEntity(Long id) {
        return subscriberRepository.findById(id).orElseThrow(() -> new NotFoundException("Subscriber not found: " + id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff not found: " + id));
    }
}