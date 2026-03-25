package ru.urasha.callmeani.blps.api.dto;

import java.math.BigDecimal;

public record TariffOptionDto(Long id, String name, String description, BigDecimal price) {
}
