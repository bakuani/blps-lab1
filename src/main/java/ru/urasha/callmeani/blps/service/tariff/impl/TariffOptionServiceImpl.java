package ru.urasha.callmeani.blps.service.tariff.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;

import ru.urasha.callmeani.blps.repository.TariffOptionRepository;
import ru.urasha.callmeani.blps.service.tariff.TariffService;
import ru.urasha.callmeani.blps.service.tariff.TariffOptionService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TariffOptionServiceImpl implements TariffOptionService {

    private final TariffOptionRepository tariffOptionRepository;
    private final TariffService tariffService;
    private final ru.urasha.callmeani.blps.mapper.FeatureMapper featureMapper;
    private final ru.urasha.callmeani.blps.mapper.SubscriberMapper subscriberMapper;
    private final ru.urasha.callmeani.blps.mapper.TariffMapper tariffMapper;

    @Transactional(readOnly = true)
    public List<TariffOptionResponse> getTariffOptions() {
        return tariffOptionRepository.findAll().stream().map(tariffMapper::toTariffOptionResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffOptionResponse getTariffOption(Long id) {
        return tariffMapper.toTariffOptionResponse(getTariffOptionEntity(id));
    }

    @Transactional
    public TariffOptionResponse createTariffOption(TariffOptionUpsertRequest request) {
        Tariff tariff = getTariffEntity(request.tariffId());
        TariffOption option = new TariffOption();
        tariffMapper.updateTariffOption(option, request);
        option.setTariff(tariff);
        return tariffMapper.toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public TariffOptionResponse updateTariffOption(Long id, TariffOptionUpsertRequest request) {
        TariffOption option = getTariffOptionEntity(id);
        Tariff tariff = getTariffEntity(request.tariffId());
        tariffMapper.updateTariffOption(option, request);
        option.setTariff(tariff);
        return tariffMapper.toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public void deleteTariffOption(Long id) {
        tariffOptionRepository.delete(getTariffOptionEntity(id));
    }

    public TariffOption getTariffOptionEntity(Long id) {
        return tariffOptionRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff option not found: " + id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffService.getTariffEntity(id);
    }
}









