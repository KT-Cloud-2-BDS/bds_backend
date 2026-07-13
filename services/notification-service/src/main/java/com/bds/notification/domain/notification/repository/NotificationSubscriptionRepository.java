package com.bds.notification.domain.notification.repository;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.model.NotificationSubscription;
import java.util.List;
import java.util.Optional;

public interface NotificationSubscriptionRepository {

  NotificationSubscription save(NotificationSubscription subscription);

  boolean existsActiveSubscription(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  Optional<NotificationSubscription> findActiveSubscription(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  List<Long> findSubscribedMemberIds(
      SubscriptionTargetType targetType,
      Long targetId);
}
