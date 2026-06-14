package ru.urasha.callmeani.blps.service.feature.camunda;

import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;

import java.util.Map;

public interface FeatureDisableCamundaTaskService {

    Map<String, CamundaVariable> createFeatureDisableRequest(LockedExternalTask task);

    Map<String, CamundaVariable> validateFeatureDisable(LockedExternalTask task);

    Map<String, CamundaVariable> disableFeatureBilling(LockedExternalTask task);

    Map<String, CamundaVariable> updateSubscriberFeature(LockedExternalTask task);

    Map<String, CamundaVariable> sendNotification(LockedExternalTask task);

    Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task);

    void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft);
}
