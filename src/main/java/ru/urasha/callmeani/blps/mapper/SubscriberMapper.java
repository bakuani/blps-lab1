package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import ru.urasha.callmeani.blps.api.dto.feature.*;
import ru.urasha.callmeani.blps.api.dto.subscriber.*;

import ru.urasha.callmeani.blps.domain.entity.*;

@Mapper(componentModel = "spring")
public interface SubscriberMapper {
    @Mapping(target = "currentTariffId", source = "currentTariff.id")
    SubscriberResponse toSubscriberResponse(Subscriber subscriber);

    @Mapping(target = "subscriberId", source = "subscriber.id")
    @Mapping(target = "featureId", source = "feature.id")
    SubscriberFeatureResponse toSubscriberFeatureResponse(SubscriberFeature subscriberFeature);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "currentTariff", ignore = true)
    void updateSubscriber(@MappingTarget Subscriber subscriber, SubscriberUpsertRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "subscriber", ignore = true)
    @Mapping(target = "feature", ignore = true)
    void updateSubscriberFeature(@MappingTarget SubscriberFeature subscriberFeature, SubscriberFeatureUpsertRequest request);

    @Mapping(target = "id", source = "feature.id")
    @Mapping(target = "name", source = "feature.name")
    @Mapping(target = "category", source = "feature.category.name")
    @Mapping(target = "monthlyFee", source = "feature.monthlyFee")
    @Mapping(target = "status", source = "status")
    FeatureSummaryDto toFeatureSummaryDto(SubscriberFeature subscriberFeature);
}
