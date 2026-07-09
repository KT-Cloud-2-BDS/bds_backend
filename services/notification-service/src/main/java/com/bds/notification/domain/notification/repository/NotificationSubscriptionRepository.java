package com.bds.notification.domain.notification.repository;

import com.bds.notification.domain.notification.entity.NotificationSubscription;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationSubscriptionRepository extends
    JpaRepository<NotificationSubscription, Long> {

  boolean existsByMemberIdAndTargetTypeAndTargetId(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  Optional<NotificationSubscription> findByMemberIdAndTargetTypeAndTargetId(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  @Query("SELECT n.memberId FROM NotificationSubscription n WHERE n.targetType = :targetType AND n.targetId = :targetId")
  List<Long> findMemberIdsByTargetTypeAndTargetId(
      SubscriptionTargetType targetType,
      Long targetId);
}
