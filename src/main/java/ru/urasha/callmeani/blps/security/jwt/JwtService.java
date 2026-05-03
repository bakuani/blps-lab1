package ru.urasha.callmeani.blps.security.jwt;

import io.github.cdimascio.dotenv.Dotenv;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import ru.urasha.callmeani.blps.security.auth.AuthenticatedUser;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class JwtService {

    private final SecretKey signingKey;
    private final Duration expiration;
    private final String issuer;

    public JwtService(
        @Value("${jwt.secret:}") String configuredSecret,
        @Value("${jwt.expiration:PT15M}") Duration expiration,
        @Value("${jwt.issuer:blps}") String issuer
    ) {
        String secret = resolveSecret(configuredSecret);
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
        this.issuer = issuer;
    }

    public String generateToken(AuthenticatedUser user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(expiration);
        List<String> authorities = user.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        List<String> roles = authorities.stream()
            .filter(value -> value.startsWith("ROLE_"))
            .map(value -> value.substring("ROLE_".length()))
            .toList();

        return Jwts.builder()
            .subject(user.getUsername())
            .issuer(issuer)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .claim("roles", roles)
            .claim("authorities", authorities)
            .claim("subscriber_id", user.getSubscriberId())
            .signWith(signingKey)
            .compact();
    }

    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    public Long extractSubscriberId(String token) {
        Object claim = parseClaims(token).get("subscriber_id");
        if (claim == null) {
            return null;
        }
        if (claim instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(claim.toString());
    }

    public List<String> extractAuthorities(String token) {
        Object raw = parseClaims(token).get("authorities");
        if (raw instanceof Collection<?> collection) {
            return collection.stream().filter(Objects::nonNull).map(Object::toString).toList();
        }
        return List.of();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = parseClaims(token);
        boolean expired = claims.getExpiration().before(new Date());
        return !expired && claims.getSubject().equals(userDetails.getUsername());
    }

    public long getExpirationSeconds() {
        return expiration.toSeconds();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey)
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private String resolveSecret(String configuredSecret) {
        if (configuredSecret != null && !configuredSecret.isBlank()) {
            return configuredSecret.trim();
        }
        String envSecret = System.getenv("JWT_SECRET");
        if (envSecret != null && !envSecret.isBlank()) {
            return envSecret.trim();
        }

        Dotenv dotenv = Dotenv.configure().ignoreIfMalformed().ignoreIfMissing().load();
        String dotenvSecret = dotenv.get("JWT_SECRET");
        if (dotenvSecret != null && !dotenvSecret.isBlank()) {
            return dotenvSecret.trim();
        }
        throw new IllegalStateException("JWT secret is not configured. Set JWT_SECRET in environment or .env.");
    }
}

