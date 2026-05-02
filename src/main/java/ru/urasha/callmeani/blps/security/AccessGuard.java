package ru.urasha.callmeani.blps.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component("accessGuard")
public class AccessGuard {

    private static final Set<String> ANY_SCOPE_AUTHORITIES = Set.of(
        Permissions.TARIFF_CHANGE_ANY,
        Permissions.FEATURE_DISABLE_ANY
    );

    public boolean canAccessSubscriber(Authentication authentication, Long subscriberId) {
        if (authentication == null || subscriberId == null) {
            return false;
        }
        boolean hasAnyScope = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(ANY_SCOPE_AUTHORITIES::contains);
        if (hasAnyScope) {
            return true;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthenticatedUser user) {
            return subscriberId.equals(user.getSubscriberId());
        }
        return false;
    }
}
