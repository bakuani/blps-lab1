package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;
import java.util.List;

public record TariffDetailsResponse(
    Long id,
    String name,
    String description,
    String category,
    BigDecimal monthlyFee,
    BigDecimal switchFee,
    boolean customizable,
    String pdfUrl,
    List<TariffOptionDto> options
) {
}
