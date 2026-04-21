package ru.urasha.callmeani.blps.api.dto.tariff;

import java.math.BigDecimal;

public record TariffResponse(
    Long id,
    String name,
    String description,
    BigDecimal monthlyFee,
    BigDecimal switchFee,
    boolean customizable,
    String pdfUrl,
    Long categoryId
) {
}



