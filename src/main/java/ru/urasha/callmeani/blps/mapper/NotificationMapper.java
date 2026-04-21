package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.urasha.callmeani.blps.api.dto.common.*;
import ru.urasha.callmeani.blps.api.dto.tariff.*;
import ru.urasha.callmeani.blps.api.dto.feature.*;
import ru.urasha.callmeani.blps.api.dto.subscriber.*;
import ru.urasha.callmeani.blps.api.dto.billing.*;
import ru.urasha.callmeani.blps.api.dto.notification.*;
import ru.urasha.callmeani.blps.domain.entity.*;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "type", source = "type")
    NotificationDto toNotificationDto(NotificationEvent notification);
}

