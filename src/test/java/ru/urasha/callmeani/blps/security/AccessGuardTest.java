package ru.urasha.callmeani.blps.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AccessGuardTest {

    private final AccessGuard accessGuard = new AccessGuard();

    @Test
    void allowsSubscriberForOwnProfile() {
        AuthenticatedUser principal = new AuthenticatedUser(
            "subscriber1",
            "password",
            10L,
            List.of(new SimpleGrantedAuthority(Permissions.TARIFF_CHANGE_OWN))
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );

        assertThat(accessGuard.canAccessSubscriber(authentication, 10L)).isTrue();
        assertThat(accessGuard.canAccessSubscriber(authentication, 11L)).isFalse();
    }

    @Test
    void allowsOperatorForAnyProfile() {
        AuthenticatedUser principal = new AuthenticatedUser(
            "operator1",
            "password",
            null,
            List.of(new SimpleGrantedAuthority(Permissions.TARIFF_CHANGE_ANY))
        );
        UsernamePasswordAuthenticationToken authentication = UsernamePasswordAuthenticationToken.authenticated(
            principal,
            null,
            principal.getAuthorities()
        );

        assertThat(accessGuard.canAccessSubscriber(authentication, 1L)).isTrue();
        assertThat(accessGuard.canAccessSubscriber(authentication, 999L)).isTrue();
    }
}
