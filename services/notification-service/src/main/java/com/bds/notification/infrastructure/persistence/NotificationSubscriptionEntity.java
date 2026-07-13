package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
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
import org.hibernate.annotations.SQLRestriction;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "notification_subscription")
@SQLRestriction("is_deleted = false")
public class NotificationSubscriptionEntity {

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

  @Column(nullable = false)
  private Boolean isDeleted = false;

  @Column(nullable = true)
  private LocalDateTime deletedAt;

  @Builder
  public NotificationSubscriptionEntity(Long subscriptionId, Long memberId,
      SubscriptionTargetType targetType, Long targetId, LocalDateTime createdAt, Boolean isDeleted,
      LocalDateTime deletedAt) {
    this.subscriptionId = subscriptionId;
    this.memberId = memberId;
    this.targetType = targetType;
    this.targetId = targetId;
    this.createdAt = createdAt;
    this.isDeleted = isDeleted;
    this.deletedAt = deletedAt;
  }
}