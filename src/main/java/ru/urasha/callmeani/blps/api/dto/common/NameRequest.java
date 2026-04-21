package ru.urasha.callmeani.blps.api.dto.common;

import jakarta.validation.constraints.NotBlank;

public record NameRequest(@NotBlank String name) {
}


