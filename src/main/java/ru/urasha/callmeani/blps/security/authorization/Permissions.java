package ru.urasha.callmeani.blps.security.authorization;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class Permissions {

    public static final String TARIFF_READ = "TARIFF_READ";
    public static final String TARIFF_WRITE = "TARIFF_WRITE";
    public static final String TARIFF_DELETE = "TARIFF_DELETE";
    public static final String TARIFF_CHANGE_OWN = "TARIFF_CHANGE_OWN";
    public static final String TARIFF_CHANGE_ANY = "TARIFF_CHANGE_ANY";

    public static final String FEATURE_READ = "FEATURE_READ";
    public static final String FEATURE_WRITE = "FEATURE_WRITE";
    public static final String FEATURE_DELETE = "FEATURE_DELETE";
    public static final String FEATURE_DISABLE_OWN = "FEATURE_DISABLE_OWN";
    public static final String FEATURE_DISABLE_ANY = "FEATURE_DISABLE_ANY";

    public static final String TARIFF_CATEGORY_READ = "TARIFF_CATEGORY_READ";
    public static final String TARIFF_CATEGORY_WRITE = "TARIFF_CATEGORY_WRITE";
    public static final String TARIFF_CATEGORY_DELETE = "TARIFF_CATEGORY_DELETE";

    public static final String TARIFF_OPTION_READ = "TARIFF_OPTION_READ";
    public static final String TARIFF_OPTION_WRITE = "TARIFF_OPTION_WRITE";
    public static final String TARIFF_OPTION_DELETE = "TARIFF_OPTION_DELETE";

    public static final String FEATURE_CATEGORY_READ = "FEATURE_CATEGORY_READ";
    public static final String FEATURE_CATEGORY_WRITE = "FEATURE_CATEGORY_WRITE";
    public static final String FEATURE_CATEGORY_DELETE = "FEATURE_CATEGORY_DELETE";

    public static final String SUBSCRIBER_READ = "SUBSCRIBER_READ";
    public static final String SUBSCRIBER_WRITE = "SUBSCRIBER_WRITE";
    public static final String SUBSCRIBER_DELETE = "SUBSCRIBER_DELETE";

    public static final String SUBSCRIBER_FEATURE_READ = "SUBSCRIBER_FEATURE_READ";
    public static final String SUBSCRIBER_FEATURE_WRITE = "SUBSCRIBER_FEATURE_WRITE";
    public static final String SUBSCRIBER_FEATURE_DELETE = "SUBSCRIBER_FEATURE_DELETE";
}

