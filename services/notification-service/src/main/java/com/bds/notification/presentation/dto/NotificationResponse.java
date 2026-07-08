package com.bds.notification.presentation.dto;

import com.bds.notification.domain.notification.entity.Notification;
import com.bds.notification.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(
    Long notificationId,
    NotificationType type,
    String title,
    String body,
    Long targetId,
    Boolean isRead,
    LocalDateTime createdAt
) {

  public static NotificationResponse from(Notification notification) {
    return new NotificationResponse(
        notification.getNotificationId(),
        notification.getType(),
        notification.getTitle(),
        notification.getBody(),
        notification.getTargetId(),
        notification.getIsRead(),
        notification.getCreatedAt()
    );
  }
}
