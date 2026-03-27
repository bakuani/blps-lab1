package ru.urasha.callmeani.blps.api.dto.admin;

import java.math.BigDecimal;

public record TariffAdminResponse(
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
