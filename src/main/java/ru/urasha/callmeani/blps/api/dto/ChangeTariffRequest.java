package ru.urasha.callmeani.blps.api.dto;

import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ChangeTariffRequest(
    @NotNull Long targetTariffId,
    Map<String, String> options
) {
}
