package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;

public record TariffSummaryDto(Long id, String name, String category, BigDecimal monthlyFee) {
}
