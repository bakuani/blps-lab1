package ru.urasha.callmeani.blps.service.eis.impl;

import org.springframework.stereotype.Service;
import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;
import ru.urasha.callmeani.blps.service.eis.EisValidationService;

@Service
public class DolibarrValidationServiceImpl implements EisValidationService {

    @Override
    public boolean allowTariffChange(TariffChangeRequest request) {
        return true;
    }

    @Override
    public boolean allowFeatureDisable(FeatureDisableRequest request) {
        return true;
    }
}
