package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.entity.NotificationChannel;
import com.bds.notification.domain.notification.entity.NotificationType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notifications")
public class NotificationEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationId;

  @Column(nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(nullable = false)
  private String targetId;

  @Column(nullable = false)
  private String title;

  @Column(nullable = false)
  private String body;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationChannel channel;

  @Column(nullable = false)
  private Boolean sendStatus;

  @Column(nullable = false)
  private Boolean isRead;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = true)
  private LocalDateTime readAt;

  @Column(nullable = true)
  private LocalDateTime clickedAt;

  @Builder
  public NotificationEntity(Long notificationId, Long memberId, NotificationType type,
      String targetId, String title, String body, NotificationChannel channel, Boolean sendStatus,
      Boolean isRead, LocalDateTime createdAt, LocalDateTime readAt, LocalDateTime clickedAt) {
    this.notificationId = notificationId;
    this.memberId = memberId;
    this.type = type;
    this.targetId = targetId;
    this.title = title;
    this.body = body;
    this.channel = channel;
    this.sendStatus = sendStatus;
    this.isRead = isRead;
    this.createdAt = createdAt;
    this.readAt = readAt;
    this.clickedAt = clickedAt;
  }
}
