package ru.urasha.callmeani.blps.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionAdminResponse;
import ru.urasha.callmeani.blps.api.dto.admin.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.exception.NotFoundException;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;
import ru.urasha.callmeani.blps.mapper.AdminMapper;
import ru.urasha.callmeani.blps.repository.TariffOptionRepository;
import ru.urasha.callmeani.blps.repository.TariffRepository;
import ru.urasha.callmeani.blps.service.AdminTariffOptionService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTariffOptionServiceImpl implements AdminTariffOptionService {

    private final TariffOptionRepository tariffOptionRepository;
    private final TariffRepository tariffRepository;
    private final AdminMapper adminMapper;

    @Transactional(readOnly = true)
    public List<TariffOptionAdminResponse> getTariffOptions() {
        return tariffOptionRepository.findAll().stream().map(adminMapper::toTariffOptionResponse).toList();
    }

    @Transactional(readOnly = true)
    public TariffOptionAdminResponse getTariffOption(Long id) {
        return adminMapper.toTariffOptionResponse(getTariffOptionEntity(id));
    }

    @Transactional
    public TariffOptionAdminResponse createTariffOption(TariffOptionUpsertRequest request) {
        Tariff tariff = getTariffEntity(request.tariffId());
        TariffOption option = new TariffOption();
        adminMapper.updateTariffOption(option, request);
        option.setTariff(tariff);
        return adminMapper.toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public TariffOptionAdminResponse updateTariffOption(Long id, TariffOptionUpsertRequest request) {
        TariffOption option = getTariffOptionEntity(id);
        Tariff tariff = getTariffEntity(request.tariffId());
        adminMapper.updateTariffOption(option, request);
        option.setTariff(tariff);
        return adminMapper.toTariffOptionResponse(tariffOptionRepository.save(option));
    }

    @Transactional
    public void deleteTariffOption(Long id) {
        tariffOptionRepository.delete(getTariffOptionEntity(id));
    }

    private TariffOption getTariffOptionEntity(Long id) {
        return tariffOptionRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff option not found: " + id));
    }

    private Tariff getTariffEntity(Long id) {
        return tariffRepository.findById(id).orElseThrow(() -> new NotFoundException("Tariff not found: " + id));
    }
}