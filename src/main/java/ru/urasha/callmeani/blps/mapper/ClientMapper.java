package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.urasha.callmeani.blps.api.dto.*;
import ru.urasha.callmeani.blps.domain.entity.*;

@Mapper(componentModel = "spring")
public interface ClientMapper {

    IdNameDto toIdNameDto(TariffCategory category);
    
    IdNameDto toIdNameDto(FeatureCategory category);

    @Mapping(target = "category", source = "category.name")
    TariffSummaryDto toTariffSummaryDto(Tariff tariff);

    TariffOptionDto toTariffOptionDto(TariffOption option);

    @Mapping(target = "category", source = "category.name")
    TariffDetailsResponse toTariffDetailsResponse(Tariff tariff);

    @Mapping(target = "id", source = "service.id")
    @Mapping(target = "name", source = "service.name")
    @Mapping(target = "category", source = "service.category.name")
    @Mapping(target = "monthlyFee", source = "service.monthlyFee")
    @Mapping(target = "status", source = "status")
    FeatureSummaryDto toFeatureSummaryDto(SubscriberFeature SubscriberFeature);

    @Mapping(target = "category", source = "category.name")
    FeatureDetailsResponse toFeatureDetailsResponse(AdditionalFeature service);

    @Mapping(target = "type", source = "type")
    BillingTransactionDto toBillingTransactionDto(BillingTransaction transaction);

    @Mapping(target = "type", source = "type")
    NotificationDto toNotificationDto(NotificationEvent notification);
}

