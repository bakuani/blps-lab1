package ru.urasha.callmeani.blps.api.dto.tariff;

import java.math.BigDecimal;

public record TariffSummaryDto(Long id, String name, String category, BigDecimal monthlyFee) {
}

