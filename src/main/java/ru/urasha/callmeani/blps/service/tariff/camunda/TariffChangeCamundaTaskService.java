package ru.urasha.callmeani.blps.service.tariff.camunda;

import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;

import java.util.Map;

public interface TariffChangeCamundaTaskService {

    Map<String, CamundaVariable> validateTariffChange(LockedExternalTask task);

    Map<String, CamundaVariable> chargeSwitchFee(LockedExternalTask task);

    Map<String, CamundaVariable> chargeNewMonthlyFee(LockedExternalTask task);

    Map<String, CamundaVariable> updateSubscriberTariff(LockedExternalTask task);

    Map<String, CamundaVariable> sendNotification(LockedExternalTask task);

    Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task);

    void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft);
}
