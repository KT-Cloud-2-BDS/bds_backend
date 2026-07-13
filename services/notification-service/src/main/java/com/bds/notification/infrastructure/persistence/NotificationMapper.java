package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.model.Notification;

public class NotificationMapper {

  public static Notification toDomain(NotificationEntity entity) {
    return Notification.from(
        entity.getNotificationId(),
        entity.getMemberId(),
        entity.getType(),
        entity.getTargetId(),
        entity.getTitle(),
        entity.getBody(),
        entity.getChannel(),
        entity.getSendStatus(),
        entity.getIsRead(),
        entity.getCreatedAt(),
        entity.getReadAt(),
        entity.getClickedAt()
    );
  }

  public static NotificationEntity toEntity(Notification model) {
    return NotificationEntity.builder()
        .notificationId(model.getNotificationId())
        .memberId(model.getMemberId())
        .type(model.getType())
        .targetId(model.getTargetId())
        .title(model.getTitle())
        .body(model.getBody())
        .channel(model.getChannel())
        .sendStatus(model.getSendStatus())
        .isRead(model.getIsRead())
        .createdAt(model.getCreatedAt())
        .readAt(model.getReadAt())
        .clickedAt(model.getClickedAt())
        .build();
  }
}
