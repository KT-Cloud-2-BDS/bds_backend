package com.bds.notification.domain.notification.model;

import com.bds.notification.domain.notification.entity.NotificationChannel;
import com.bds.notification.domain.notification.entity.NotificationType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class Notification {

  private Long notificationId;

  private Long memberId;

  private NotificationType type;

  private String targetId;

  private String title;

  private String body;

  private NotificationChannel channel;

  private Boolean sendStatus = false;

  private Boolean isRead = false;

  private LocalDateTime createdAt;

  private LocalDateTime readAt;

  private LocalDateTime clickedAt;

  public static Notification create(
      Long memberId, NotificationType type, String targetId, String title, String body,
      NotificationChannel channel
  ) {
    Notification notification = new Notification();
    notification.memberId = memberId;
    notification.type = type;
    notification.targetId = targetId;
    notification.title = title;
    notification.body = body;
    notification.channel = channel;
    notification.createdAt = LocalDateTime.now();

    return notification;
  }

  public static Notification from(Long notificationId, Long memberId, NotificationType type,
      String targetId,
      String title, String body, NotificationChannel channel, Boolean sendStatus, Boolean isRead,
      LocalDateTime createdAt, LocalDateTime readAt, LocalDateTime clickedAt) {
    Notification notification = new Notification();
    notification.notificationId = notificationId;
    notification.memberId = memberId;
    notification.type = type;
    notification.targetId = targetId;
    notification.title = title;
    notification.body = body;
    notification.channel = channel;
    notification.sendStatus = sendStatus;
    notification.isRead = isRead;
    notification.createdAt = createdAt;
    notification.readAt = readAt;
    notification.clickedAt = clickedAt;

    return notification;
  }
}
