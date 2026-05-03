package ru.urasha.callmeani.blps.security.auth;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

@Getter
public class AuthenticatedUser extends User {

    private final Long subscriberId;

    public AuthenticatedUser(
        String username,
        String password,
        Long subscriberId,
        Collection<? extends GrantedAuthority> authorities
    ) {
        super(username, password, authorities);
        this.subscriberId = subscriberId;
    }

}

