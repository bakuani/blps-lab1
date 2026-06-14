package ru.urasha.callmeani.blps.service.billing.camunda;

import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;

import java.util.Map;

public interface MonthlyFeeCamundaTaskService {

    Map<String, CamundaVariable> createMonthlyFeeRequests();

    Map<String, CamundaVariable> ensureMonthlyFeeRequest(LockedExternalTask task);

    Map<String, CamundaVariable> createDolibarrInvoice(LockedExternalTask task);

    Map<String, CamundaVariable> chargeMonthlyFee(LockedExternalTask task);

    Map<String, CamundaVariable> syncDolibarrInvoice(LockedExternalTask task);

    Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task);

    void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft);
}
