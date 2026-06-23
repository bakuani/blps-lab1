package ru.urasha.callmeani.blps.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.urasha.callmeani.blps.api.dto.notification.NotificationDto;
import ru.urasha.callmeani.blps.domain.entity.NotificationEvent;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    @Mapping(target = "type", source = "type")
    NotificationDto toNotificationDto(NotificationEvent notification);
}

