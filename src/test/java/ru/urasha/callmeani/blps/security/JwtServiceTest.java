package ru.urasha.callmeani.blps.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    @Test
    void generatesAndValidatesToken() {
        JwtService jwtService = new JwtService(
            "12345678901234567890123456789012",
            Duration.ofMinutes(15),
            "blps-test"
        );
        AuthenticatedUser user = new AuthenticatedUser(
            "subscriber1",
            "password",
            77L,
            List.of(new SimpleGrantedAuthority(Permissions.TARIFF_READ))
        );

        String token = jwtService.generateToken(user);

        assertThat(token).isNotBlank();
        assertThat(jwtService.extractUsername(token)).isEqualTo("subscriber1");
        assertThat(jwtService.extractSubscriberId(token)).isEqualTo(77L);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }
}
