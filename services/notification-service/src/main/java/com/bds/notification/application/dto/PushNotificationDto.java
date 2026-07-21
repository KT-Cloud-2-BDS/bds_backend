package com.bds.notification.application.dto;

import com.bds.notification.domain.notification.entity.NotificationType;
import com.bds.notification.domain.notification.model.Notification;

public record PushNotificationDto(
    NotificationType type,
    String title,
    String body
) {

  public static PushNotificationDto from(Notification notification) {
    return new PushNotificationDto(
        notification.getType(),
        notification.getTitle(),
        notification.getBody()
    );
  }
}
