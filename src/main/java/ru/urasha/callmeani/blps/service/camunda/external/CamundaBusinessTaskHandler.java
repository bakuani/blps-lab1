package ru.urasha.callmeani.blps.service.camunda.external;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;
import ru.urasha.callmeani.blps.service.camunda.model.LockedExternalTask;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessConstants;
import ru.urasha.callmeani.blps.service.camunda.process.CamundaProcessVariables;
import ru.urasha.callmeani.blps.service.billing.camunda.MonthlyFeeCamundaTaskService;
import ru.urasha.callmeani.blps.service.feature.camunda.FeatureDisableCamundaTaskService;
import ru.urasha.callmeani.blps.service.tariff.camunda.TariffChangeCamundaTaskService;

import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CamundaBusinessTaskHandler implements CamundaExternalTaskHandler {

    private static final String TARIFF_CHANGE = CamundaProcessConstants.OPERATION_TARIFF_CHANGE;
    private static final String FEATURE_DISABLE = CamundaProcessConstants.OPERATION_FEATURE_DISABLE;
    private static final String MONTHLY_FEE = CamundaProcessConstants.OPERATION_MONTHLY_FEE;

    private final TariffChangeCamundaTaskService tariffChangeTaskService;
    private final FeatureDisableCamundaTaskService featureDisableTaskService;
    private final MonthlyFeeCamundaTaskService monthlyFeeTaskService;

    @Override
    public Set<String> topics() {
        return Set.of(
            CamundaProcessConstants.VALIDATE_TARIFF_CHANGE,
            CamundaProcessConstants.CHARGE_SWITCH_FEE,
            CamundaProcessConstants.CHARGE_NEW_MONTHLY_FEE,
            CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF,
            CamundaProcessConstants.VALIDATE_FEATURE_DISABLE,
            CamundaProcessConstants.DISABLE_FEATURE_BILLING,
            CamundaProcessConstants.UPDATE_SUBSCRIBER_FEATURE,
            CamundaProcessConstants.CREATE_MONTHLY_FEE_REQUESTS,
            CamundaProcessConstants.CREATE_DOLIBARR_INVOICE,
            CamundaProcessConstants.CHARGE_MONTHLY_FEE,
            CamundaProcessConstants.SYNC_DOLIBARR_INVOICE,
            CamundaProcessConstants.SEND_NOTIFICATION,
            CamundaProcessConstants.PUBLISH_EIS_AUDIT
        );
    }

    @Override
    public Map<String, CamundaVariable> handle(LockedExternalTask task) {
        return switch (task.topicName()) {
            case CamundaProcessConstants.VALIDATE_TARIFF_CHANGE -> tariffChangeTaskService.validateTariffChange(task);
            case CamundaProcessConstants.CHARGE_SWITCH_FEE -> tariffChangeTaskService.chargeSwitchFee(task);
            case CamundaProcessConstants.CHARGE_NEW_MONTHLY_FEE -> tariffChangeTaskService.chargeNewMonthlyFee(task);
            case CamundaProcessConstants.UPDATE_SUBSCRIBER_TARIFF -> tariffChangeTaskService.updateSubscriberTariff(task);
            case CamundaProcessConstants.VALIDATE_FEATURE_DISABLE -> featureDisableTaskService.validateFeatureDisable(task);
            case CamundaProcessConstants.DISABLE_FEATURE_BILLING -> featureDisableTaskService.disableFeatureBilling(task);
            case CamundaProcessConstants.UPDATE_SUBSCRIBER_FEATURE -> featureDisableTaskService.updateSubscriberFeature(task);
            case CamundaProcessConstants.CREATE_MONTHLY_FEE_REQUESTS -> monthlyFeeTaskService.createMonthlyFeeRequests();
            case CamundaProcessConstants.CREATE_DOLIBARR_INVOICE -> monthlyFeeTaskService.createDolibarrInvoice(task);
            case CamundaProcessConstants.CHARGE_MONTHLY_FEE -> monthlyFeeTaskService.chargeMonthlyFee(task);
            case CamundaProcessConstants.SYNC_DOLIBARR_INVOICE -> monthlyFeeTaskService.syncDolibarrInvoice(task);
            case CamundaProcessConstants.SEND_NOTIFICATION -> sendNotification(task);
            case CamundaProcessConstants.PUBLISH_EIS_AUDIT -> publishEisAudit(task);
            default -> throw new IllegalArgumentException("Unsupported Camunda topic: " + task.topicName());
        };
    }

    @Override
    public void handleFailure(LockedExternalTask task, RuntimeException exception, int retriesLeft) {
        String operationType = task.stringVariable(CamundaProcessVariables.OPERATION_TYPE);
        if (TARIFF_CHANGE.equals(operationType)) {
            tariffChangeTaskService.handleFailure(task, exception, retriesLeft);
        } else if (FEATURE_DISABLE.equals(operationType)) {
            featureDisableTaskService.handleFailure(task, exception, retriesLeft);
        } else if (MONTHLY_FEE.equals(operationType)) {
            monthlyFeeTaskService.handleFailure(task, exception, retriesLeft);
        }
    }

    private Map<String, CamundaVariable> sendNotification(LockedExternalTask task) {
        String operationType = task.stringVariable(CamundaProcessVariables.OPERATION_TYPE);
        if (TARIFF_CHANGE.equals(operationType)) {
            return tariffChangeTaskService.sendNotification(task);
        }
        if (FEATURE_DISABLE.equals(operationType)) {
            return featureDisableTaskService.sendNotification(task);
        }
        return Map.of();
    }

    private Map<String, CamundaVariable> publishEisAudit(LockedExternalTask task) {
        String operationType = task.stringVariable(CamundaProcessVariables.OPERATION_TYPE);
        if (TARIFF_CHANGE.equals(operationType)) {
            return tariffChangeTaskService.publishEisAudit(task);
        }
        if (FEATURE_DISABLE.equals(operationType)) {
            return featureDisableTaskService.publishEisAudit(task);
        }
        if (MONTHLY_FEE.equals(operationType)) {
            return monthlyFeeTaskService.publishEisAudit(task);
        }
        return Map.of();
    }
}
