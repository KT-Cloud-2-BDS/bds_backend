package com.bds.notification.domain.notification.entity;

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
public class Notification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long notificationId;

  @Column(nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private NotificationType type;

  @Column(nullable = false)
  private Long targetId;

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
  private Notification(Long memberId, NotificationType type, Long targetId,
      String title, String body, NotificationChannel channel) {
    this.memberId = memberId;
    this.type = type;
    this.targetId = targetId;
    this.title = title;
    this.body = body;
    this.channel = channel;
    this.sendStatus = false;
    this.isRead = false;
    this.createdAt = LocalDateTime.now();
  }
}
