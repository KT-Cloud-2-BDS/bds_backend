package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface NotificationSubscriptionJpaRepository
    extends JpaRepository<NotificationSubscriptionEntity, Long> {

  boolean existsByMemberIdAndTargetTypeAndTargetId(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  Optional<NotificationSubscriptionEntity> findByMemberIdAndTargetTypeAndTargetId(
      Long memberId,
      SubscriptionTargetType targetType,
      Long targetId);

  @Query("SELECT n.memberId FROM NotificationSubscriptionEntity n WHERE n.targetType = :targetType AND n.targetId = :targetId")
  List<Long> findMemberIdsByTargetTypeAndTargetId(
      SubscriptionTargetType targetType,
      Long targetId);
}
