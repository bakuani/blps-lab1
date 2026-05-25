package ru.urasha.callmeani.blps.api.message;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ApiMessages {

    public static final String MALFORMED_JSON_REQUEST = "Malformed JSON request";
    public static final String INVALID_PARAMETER_PREFIX = "Invalid value for parameter: ";
    public static final String DATA_INTEGRITY_VIOLATION = "Data integrity violation";
    public static final String INVALID_CREDENTIALS = "Invalid credentials";
    public static final String UNAUTHORIZED = "Unauthorized";
    public static final String FORBIDDEN = "Forbidden";
    public static final String INTERNAL_SERVER_ERROR = "Internal server error";

    public static final String JWT_INVALID_TOKEN = "Invalid JWT token";
    public static final String JWT_INVALID_CLAIMS = "Invalid JWT claims";
    public static final String JWT_SECRET_NOT_CONFIGURED =
        "JWT secret is not configured. Set JWT_SECRET in environment or .env.";

    public static final String TARIFF_ALREADY_SELECTED_NOTIFICATION =
        "Current tariff is already selected. Change request rejected.";
    public static final String TARIFF_ALREADY_SELECTED_RESPONSE = "Current tariff is already selected";
    public static final String TARIFF_INSUFFICIENT_FUNDS_NOTIFICATION = "Insufficient funds for tariff change.";
    public static final String TARIFF_INSUFFICIENT_FUNDS_RESPONSE = "Insufficient funds";
    public static final String TARIFF_SWITCH_FEE_DESCRIPTION = "Tariff switch fee";
    public static final String TARIFF_MONTHLY_FEE_DESCRIPTION = "Monthly fee for target tariff";
    public static final String TARIFF_CHANGED_NOTIFICATION = "Tariff changed successfully.";
    public static final String TARIFF_CHANGED_RESPONSE = "Tariff changed successfully";
    public static final String TARIFF_CHANGE_REQUEST_ACCEPTED = "Tariff change request accepted for async processing";
    public static final String TARIFF_CHANGE_REJECTED_BY_EIS = "Tariff change rejected by external EIS validation";

    public static final String FEATURE_NOT_ACTIVE_NOTIFICATION = "Feature is not connected or already disabled.";
    public static final String FEATURE_NOT_ACTIVE_RESPONSE = "Feature is not active for subscriber";
    public static final String FEATURE_DISABLE_BILLING_DESCRIPTION = "Billing request for feature disable operation";
    public static final String FEATURE_DISABLED_NOTIFICATION = "Feature disabled successfully.";
    public static final String FEATURE_DISABLED_RESPONSE = "Feature disabled successfully";
    public static final String FEATURE_DISABLE_REQUEST_ACCEPTED = "Feature disable request accepted for async processing";
    public static final String FEATURE_DISABLE_REJECTED_BY_EIS = "Feature disable rejected by external EIS validation";
}
