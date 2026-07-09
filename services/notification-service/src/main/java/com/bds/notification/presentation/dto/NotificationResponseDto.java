package com.bds.notification.presentation.dto;

import com.bds.notification.domain.notification.entity.Notification;
import com.bds.notification.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponseDto(
    Long notificationId,
    NotificationType type,
    String title,
    String body,
    Long targetId,
    Boolean isRead,
    LocalDateTime createdAt
) {

  public static NotificationResponseDto from(Notification notification) {
    return new NotificationResponseDto(
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
