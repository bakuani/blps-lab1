package ru.urasha.callmeani.blps.api.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SubscriberUpsertRequest(
    @NotBlank String phone,
    @NotBlank String fullName,
    @NotNull @DecimalMin(value = "0.00") BigDecimal balance,
    Long currentTariffId
) {
}
