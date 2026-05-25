package ru.urasha.callmeani.blps.service.eis;

import ru.urasha.callmeani.blps.domain.entity.FeatureDisableRequest;
import ru.urasha.callmeani.blps.domain.entity.TariffChangeRequest;

public interface EisValidationService {
    boolean allowTariffChange(TariffChangeRequest request);
    boolean allowFeatureDisable(FeatureDisableRequest request);
}
