package ru.urasha.callmeani.blps.service.camunda.process;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import ru.urasha.callmeani.blps.service.camunda.model.CamundaVariable;

import java.util.LinkedHashMap;
import java.util.Map;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CamundaProcessVariables {

    public static final String REQUEST_ID = "requestId";
    public static final String SUBSCRIBER_ID = "subscriberId";
    public static final String TARGET_TARIFF_ID = "targetTariffId";
    public static final String FEATURE_ID = "featureId";
    public static final String BILLING_PERIOD = "billingPeriod";
    public static final String OPTIONS = "options";
    public static final String OPERATION_TYPE = "operationType";
    public static final String OPERATION_AMOUNT = "operationAmount";
    public static final String CAN_CHANGE_TARIFF = "canChangeTariff";
    public static final String HAS_SWITCH_FEE = "hasSwitchFee";
    public static final String CAN_DISABLE_FEATURE = "canDisableFeature";
    public static final String OPERATION_SUCCESS = "operationSuccess";
    public static final String CREATED_MONTHLY_FEE_REQUESTS = "createdMonthlyFeeRequests";
    public static final String MONTHLY_FEE_TERMINAL = "monthlyFeeTerminal";
    public static final String MONTHLY_FEE_SUCCEEDED = "monthlyFeeSucceeded";

    public static Map<String, CamundaVariable> of(Object... keyValues) {
        if (keyValues.length % 2 != 0) {
            throw new IllegalArgumentException("Camunda variables must be passed as key-value pairs");
        }

        Map<String, CamundaVariable> variables = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!(keyValues[i] instanceof String key)) {
                throw new IllegalArgumentException("Camunda variable key must be a String");
            }
            if (!(keyValues[i + 1] instanceof CamundaVariable value)) {
                throw new IllegalArgumentException("Camunda variable value must be a CamundaVariable");
            }
            variables.put(key, value);
        }
        return variables;
    }
}
