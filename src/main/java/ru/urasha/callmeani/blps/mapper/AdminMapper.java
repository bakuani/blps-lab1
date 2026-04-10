package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.urasha.callmeani.blps.api.dto.admin.*;
import ru.urasha.callmeani.blps.domain.entity.*;

@Mapper(componentModel = "spring")
public interface AdminMapper {

    IdNameResponse toIdNameResponse(TariffCategory category);
    IdNameResponse toIdNameResponse(FeatureCategory category);

    @Mapping(target = "categoryId", source = "category.id")
    TariffAdminResponse toTariffResponse(Tariff tariff);

    @Mapping(target = "tariffId", source = "tariff.id")
    TariffOptionAdminResponse toTariffOptionResponse(TariffOption option);

    @Mapping(target = "categoryId", source = "category.id")
    AdditionalFeatureAdminResponse toServiceResponse(AdditionalFeature service);

    @Mapping(target = "currentTariffId", source = "currentTariff.id")
    SubscriberAdminResponse toSubscriberResponse(Subscriber subscriber);

    @Mapping(target = "subscriberId", source = "subscriber.id")
    @Mapping(target = "serviceId", source = "service.id")
    SubscriberFeatureAdminResponse toSubscriberFeatureResponse(SubscriberFeature SubscriberFeature);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "options", ignore = true)
    void updateTariff(@MappingTarget Tariff tariff, TariffUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "tariff", ignore = true)
    void updateTariffOption(@MappingTarget TariffOption option, TariffOptionUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "category", ignore = true)
    void updateAdditionalFeature(@MappingTarget AdditionalFeature service, AdditionalFeatureUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentTariff", ignore = true)
    void updateSubscriber(@MappingTarget Subscriber subscriber, SubscriberUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriber", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateSubscriberFeature(@MappingTarget SubscriberFeature SubscriberFeature, SubscriberFeatureUpsertRequest request);
}
