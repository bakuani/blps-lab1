package ru.urasha.callmeani.blps.api.dto.auth;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds
) {
}
