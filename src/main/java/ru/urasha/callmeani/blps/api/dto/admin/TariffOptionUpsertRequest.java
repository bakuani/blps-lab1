package ru.urasha.callmeani.blps.api.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TariffOptionUpsertRequest(
    @NotBlank String name,
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.00") BigDecimal price,
    @NotNull Long tariffId
) {
}
