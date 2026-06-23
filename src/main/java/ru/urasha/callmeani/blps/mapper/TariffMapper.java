package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.urasha.callmeani.blps.api.dto.common.IdNameDto;
import ru.urasha.callmeani.blps.api.dto.common.IdNameResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffDetailsResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffOptionUpsertRequest;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffResponse;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffSummaryDto;
import ru.urasha.callmeani.blps.api.dto.tariff.TariffUpsertRequest;
import ru.urasha.callmeani.blps.domain.entity.Tariff;
import ru.urasha.callmeani.blps.domain.entity.TariffCategory;
import ru.urasha.callmeani.blps.domain.entity.TariffOption;

@Mapper(componentModel = "spring")
public interface TariffMapper {
    IdNameResponse toIdNameResponse(TariffCategory category);

    @Mapping(target = "categoryId", source = "category.id")
    TariffResponse toTariffResponse(Tariff tariff);

    @Mapping(target = "tariffId", source = "tariff.id")
    TariffOptionResponse toTariffOptionResponse(TariffOption option);
    
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "options", ignore = true)
    void updateTariff(@MappingTarget Tariff tariff, TariffUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tariff", ignore = true)
    void updateTariffOption(@MappingTarget TariffOption option, TariffOptionUpsertRequest request);

    IdNameDto toIdNameDto(TariffCategory category);

    @Mapping(target = "category", source = "category.name")
    TariffSummaryDto toTariffSummaryDto(Tariff tariff);

    TariffOptionDto toTariffOptionDto(TariffOption option);

    @Mapping(target = "category", source = "category.name")
    TariffDetailsResponse toTariffDetailsResponse(Tariff tariff);
}

