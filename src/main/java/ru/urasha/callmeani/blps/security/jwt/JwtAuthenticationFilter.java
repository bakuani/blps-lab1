package ru.urasha.callmeani.blps.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.urasha.callmeani.blps.api.dto.common.ApiErrorResponse;
import ru.urasha.callmeani.blps.api.message.ApiMessages;
import ru.urasha.callmeani.blps.logging.LoggingContext;
import ru.urasha.callmeani.blps.security.auth.AuthenticatedUser;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_ACTION;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_CATEGORY;
import static ru.urasha.callmeani.blps.logging.LoggingFields.EVENT_OUTCOME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.SUBSCRIBER_ID;
import static ru.urasha.callmeani.blps.logging.LoggingFields.USER_NAME;
import static ru.urasha.callmeani.blps.logging.LoggingFields.USER_ROLES;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NotNull HttpServletResponse response,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        try {
            if (!authenticateToken(token, request, response)) {
                return;
            }
        } catch (Exception ex) {
            try (LoggingContext ignored = LoggingContext.open(
                EVENT_CATEGORY, "authentication",
                EVENT_ACTION, "jwt_validation",
                EVENT_OUTCOME, "failure"
            )) {
                log.warn("JWT authentication failed: exceptionType={}", ex.getClass().getSimpleName());
            }
            writeUnauthorized(response, ApiMessages.JWT_INVALID_TOKEN);
            return;
        }
        filterChain.doFilter(request, response);
    }

    private boolean authenticateToken(
        String token,
        HttpServletRequest request,
        HttpServletResponse response
    ) throws IOException {
        String username = jwtService.extractUsername(token);
        if (username == null || SecurityContextHolder.getContext().getAuthentication() != null) {
            return true;
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(username);
        if (!jwtService.isTokenValid(token, userDetails)) {
            logAuthenticationFailure("jwt_invalid");
            writeUnauthorized(response, ApiMessages.JWT_INVALID_TOKEN);
            return false;
        }

        if (userDetails instanceof AuthenticatedUser user) {
            Long tokenSubscriberId = jwtService.extractSubscriberId(token);
            if (tokenSubscriberId != null && !tokenSubscriberId.equals(user.getSubscriberId())) {
                logAuthenticationFailure("jwt_claims_invalid");
                writeUnauthorized(response, ApiMessages.JWT_INVALID_CLAIMS);
                return false;
            }
        }

        UsernamePasswordAuthenticationToken authentication =
            UsernamePasswordAuthenticationToken.authenticated(
                userDetails,
                null,
                userDetails.getAuthorities()
            );
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        putUserContext(userDetails);
        return true;
    }

    private void putUserContext(UserDetails userDetails) {
        LoggingContext.put(USER_NAME, userDetails.getUsername());
        LoggingContext.put(
            USER_ROLES,
            userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .sorted()
                .collect(Collectors.joining(","))
        );
        if (userDetails instanceof AuthenticatedUser user) {
            LoggingContext.put(SUBSCRIBER_ID, user.getSubscriberId());
        }
    }

    private void logAuthenticationFailure(String reason) {
        try (LoggingContext ignored = LoggingContext.open(
            EVENT_CATEGORY, "authentication",
            EVENT_ACTION, "jwt_validation",
            EVENT_OUTCOME, "failure"
        )) {
            log.warn("JWT authentication rejected: reason={}", reason);
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiErrorResponse error = new ApiErrorResponse(OffsetDateTime.now(), message);
        objectMapper.writeValue(response.getOutputStream(), error);
    }
}
