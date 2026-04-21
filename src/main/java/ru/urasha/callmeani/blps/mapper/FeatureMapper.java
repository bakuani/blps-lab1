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
public interface FeatureMapper {
    IdNameResponse toIdNameResponse(FeatureCategory category);

    @Mapping(target = "categoryId", source = "category.id")
    AdditionalFeatureResponse toFeatureResponse(AdditionalFeature feature);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateAdditionalFeature(@MappingTarget AdditionalFeature feature, AdditionalFeatureUpsertRequest request);

    IdNameDto toIdNameDto(FeatureCategory category);

    @Mapping(target = "category", source = "category.name")
    FeatureDetailsResponse toFeatureDetailsResponse(AdditionalFeature feature);
}

