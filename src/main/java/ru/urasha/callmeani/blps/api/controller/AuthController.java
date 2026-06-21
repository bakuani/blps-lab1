package ru.urasha.callmeani.blps.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.urasha.callmeani.blps.api.dto.auth.LoginRequest;
import ru.urasha.callmeani.blps.api.dto.auth.LoginResponse;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.security.auth.AuthenticatedUser;
import ru.urasha.callmeani.blps.security.jwt.JwtService;

import java.util.stream.Collectors;

import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.USER_NAME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.USER_ROLES;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "authentication",
            EVENT_ACTION, "user_login",
            USER_NAME, request.username()
        )) {
            try {
                Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.username(), request.password())
                );
                AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
                LoggingContext.put(SUBSCRIBER_ID, user.getSubscriberId());
                LoggingContext.put(
                    USER_ROLES,
                    user.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .sorted()
                        .collect(Collectors.joining(","))
                );
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "success")) {
                    log.info("User login succeeded");
                }
                String token = jwtService.generateToken(user);
                return new LoginResponse(token, "Bearer", jwtService.getExpirationSeconds());
            } catch (AuthenticationException ex) {
                try (LoggingContext outcome = LoggingContext.open(EVENT_OUTCOME, "failure")) {
                    log.warn("User login failed");
                }
                throw ex;
            }
        }
    }
}

