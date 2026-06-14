package ru.urasha.callmeani.blps.service.camunda.process;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CamundaProcessConstants {

    public static final String TARIFF_CHANGE_PROCESS = "TariffChangeProcess";
    public static final String FEATURE_DISABLE_PROCESS = "FeatureDisableProcess";
    public static final String MONTHLY_FEE_CHARGE_PROCESS = "MonthlyFeeChargeProcess";

    public static final String OPERATION_TARIFF_CHANGE = "tariff-change";
    public static final String OPERATION_FEATURE_DISABLE = "feature-disable";
    public static final String OPERATION_MONTHLY_FEE = "monthly-fee";

    public static final String CREATE_TARIFF_CHANGE_REQUEST = "create-tariff-change-request";
    public static final String VALIDATE_TARIFF_CHANGE = "validate-tariff-change";
    public static final String CHARGE_SWITCH_FEE = "charge-switch-fee";
    public static final String CHARGE_NEW_MONTHLY_FEE = "charge-new-monthly-fee";
    public static final String UPDATE_SUBSCRIBER_TARIFF = "update-subscriber-tariff";
    public static final String CREATE_FEATURE_DISABLE_REQUEST = "create-feature-disable-request";
    public static final String VALIDATE_FEATURE_DISABLE = "validate-feature-disable";
    public static final String DISABLE_FEATURE_BILLING = "disable-feature-billing";
    public static final String UPDATE_SUBSCRIBER_FEATURE = "update-subscriber-feature";
    public static final String CREATE_MONTHLY_FEE_REQUESTS = "create-monthly-fee-requests";
    public static final String ENSURE_MONTHLY_FEE_REQUEST = "ensure-monthly-fee-request";
    public static final String CREATE_DOLIBARR_INVOICE = "create-dolibarr-invoice";
    public static final String CHARGE_MONTHLY_FEE = "charge-monthly-fee";
    public static final String SYNC_DOLIBARR_INVOICE = "sync-dolibarr-invoice";
    public static final String SEND_NOTIFICATION = "send-notification";
    public static final String PUBLISH_EIS_AUDIT = "publish-eis-audit";
}
