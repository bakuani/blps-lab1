package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.urasha.callmeani.blps.api.dto.common.*;
import ru.urasha.callmeani.blps.api.dto.tariff.*;
import ru.urasha.callmeani.blps.api.dto.feature.*;
import ru.urasha.callmeani.blps.api.dto.subscriber.*;
import ru.urasha.callmeani.blps.api.dto.billing.*;
import ru.urasha.callmeani.blps.api.dto.notification.*;

import ru.urasha.callmeani.blps.domain.entity.*;

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

