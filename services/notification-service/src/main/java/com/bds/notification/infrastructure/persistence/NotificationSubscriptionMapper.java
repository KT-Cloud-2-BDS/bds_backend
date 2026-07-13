package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.model.NotificationSubscription;

public class NotificationSubscriptionMapper {

  public static NotificationSubscriptionEntity toEntity(NotificationSubscription model) {
    return NotificationSubscriptionEntity.builder()
        .subscriptionId(model.getSubscriptionId())
        .memberId(model.getMemberId())
        .targetType(model.getTargetType())
        .targetId(model.getTargetId())
        .createdAt(model.getCreatedAt())
        .isDeleted(model.getIsDeleted())
        .deletedAt(model.getDeletedAt())
        .build();
  }

  public static NotificationSubscription toDomain(NotificationSubscriptionEntity entity) {
    return NotificationSubscription.from(
        entity.getSubscriptionId(),
        entity.getMemberId(),
        entity.getTargetType(),
        entity.getTargetId(),
        entity.getCreatedAt(),
        entity.getIsDeleted(),
        entity.getDeletedAt()
    );
  }
}
