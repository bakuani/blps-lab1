package ru.urasha.callmeani.blps.eis.model;

public enum EisOperationType {
    TARIFF_CHANGE_REQUESTED("TariffChangeRequested"),
    FEATURE_DISABLE_REQUESTED("FeatureDisableRequested"),
    MONTHLY_FEE_CHARGE_REQUESTED("MonthlyFeeChargeRequested");

    private final String externalCode;

    EisOperationType(String externalCode) {
        this.externalCode = externalCode;
    }

    public String externalCode() {
        return externalCode;
    }
}
