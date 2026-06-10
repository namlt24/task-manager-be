package com.taskmanager.notification.mapper;

import com.taskmanager.notification.dto.NotificationDto;
import com.taskmanager.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationDto toDto(Notification notification);
}
