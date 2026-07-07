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
@Table(name = "notification_subscription")
public class NotificationSubscription {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long subscriptionId;

  @Column(nullable = false)
  private Long memberId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private SubscriptionTargetType targetType;

  @Column(nullable = false)
  private Long targetId;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Builder
  private NotificationSubscription(Long memberId, SubscriptionTargetType targetType, Long targetId) {
    this.memberId = memberId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.createdAt = LocalDateTime.now();
  }
}