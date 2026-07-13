package com.bds.notification.domain.notification.model;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationSubscription {

  private Long subscriptionId;

  private Long memberId;

  private SubscriptionTargetType targetType;

  private Long targetId;

  private LocalDateTime createdAt;

  private Boolean isDeleted = false;

  private LocalDateTime deletedAt;

  // 새 구독 생성 시 사용
  public static NotificationSubscription create(Long memberId, SubscriptionTargetType targetType,
      Long targetId) {
    NotificationSubscription subscription = new NotificationSubscription();
    subscription.memberId = memberId;
    subscription.targetType = targetType;
    subscription.targetId = targetId;
    subscription.createdAt = LocalDateTime.now();
    return subscription;
  }

  // 엔티티 -> 도메인 모델 변환 시 사용
  public static NotificationSubscription from(Long subscriptionId, Long memberId,
      SubscriptionTargetType targetType, Long targetId, LocalDateTime createdAt,
      Boolean isDeleted, LocalDateTime deletedAt) {
    NotificationSubscription subscription = new NotificationSubscription();
    subscription.subscriptionId = subscriptionId;
    subscription.memberId = memberId;
    subscription.targetType = targetType;
    subscription.targetId = targetId;
    subscription.createdAt = createdAt;
    subscription.isDeleted = isDeleted;
    subscription.deletedAt = deletedAt;
    return subscription;
  }

  public void softDelete() {
    this.isDeleted = true;
    this.deletedAt = LocalDateTime.now();
  }
}
