package ru.urasha.callmeani.blps.security.authorization;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RolePrivileges {

    public static Set<String> resolvePermissions(List<String> roles) {
        Set<String> permissions = new LinkedHashSet<>();
        for (String role : roles) {
            switch (role) {
                case "SUBSCRIBER" -> {
                    permissions.add(Permissions.TARIFF_READ);
                    permissions.add(Permissions.TARIFF_CHANGE_OWN);
                    permissions.add(Permissions.FEATURE_READ);
                    permissions.add(Permissions.FEATURE_DISABLE_OWN);
                }
                case "OPERATOR" -> {
                    permissions.add(Permissions.TARIFF_READ);
                    permissions.add(Permissions.TARIFF_CHANGE_ANY);
                    permissions.add(Permissions.FEATURE_READ);
                    permissions.add(Permissions.FEATURE_DISABLE_ANY);
                }
                case "ADMIN" -> {
                    permissions.add(Permissions.TARIFF_READ);
                    permissions.add(Permissions.TARIFF_WRITE);
                    permissions.add(Permissions.TARIFF_DELETE);
                    permissions.add(Permissions.TARIFF_CHANGE_ANY);
                    permissions.add(Permissions.FEATURE_READ);
                    permissions.add(Permissions.FEATURE_WRITE);
                    permissions.add(Permissions.FEATURE_DELETE);
                    permissions.add(Permissions.FEATURE_DISABLE_ANY);
                    permissions.add(Permissions.TARIFF_CATEGORY_READ);
                    permissions.add(Permissions.TARIFF_CATEGORY_WRITE);
                    permissions.add(Permissions.TARIFF_CATEGORY_DELETE);
                    permissions.add(Permissions.TARIFF_OPTION_READ);
                    permissions.add(Permissions.TARIFF_OPTION_WRITE);
                    permissions.add(Permissions.TARIFF_OPTION_DELETE);
                    permissions.add(Permissions.FEATURE_CATEGORY_READ);
                    permissions.add(Permissions.FEATURE_CATEGORY_WRITE);
                    permissions.add(Permissions.FEATURE_CATEGORY_DELETE);
                    permissions.add(Permissions.SUBSCRIBER_READ);
                    permissions.add(Permissions.SUBSCRIBER_WRITE);
                    permissions.add(Permissions.SUBSCRIBER_DELETE);
                    permissions.add(Permissions.SUBSCRIBER_FEATURE_READ);
                    permissions.add(Permissions.SUBSCRIBER_FEATURE_WRITE);
                    permissions.add(Permissions.SUBSCRIBER_FEATURE_DELETE);
                }
                default -> throw new IllegalArgumentException("Unsupported role: " + role);
            }
        }
        return permissions;
    }
}

