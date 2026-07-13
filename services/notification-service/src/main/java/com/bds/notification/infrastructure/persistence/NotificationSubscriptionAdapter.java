package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.model.NotificationSubscription;
import com.bds.notification.domain.notification.repository.NotificationSubscriptionRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationSubscriptionAdapter implements NotificationSubscriptionRepository {

  private final NotificationSubscriptionJpaRepository jpaRepository;

  @Override
  public NotificationSubscription save(NotificationSubscription subscription) {
    return NotificationSubscriptionMapper.toDomain(
        jpaRepository.save(NotificationSubscriptionMapper.toEntity(subscription)));
  }

  @Override
  public boolean existsActiveSubscription(Long memberId, SubscriptionTargetType targetType,
      Long targetId) {
    return jpaRepository.existsByMemberIdAndTargetTypeAndTargetId(memberId, targetType, targetId);
  }

  @Override
  public Optional<NotificationSubscription> findActiveSubscription(Long memberId,
      SubscriptionTargetType targetType, Long targetId) {
    return jpaRepository.findByMemberIdAndTargetTypeAndTargetId(memberId, targetType, targetId)
        .map(NotificationSubscriptionMapper::toDomain);
  }

  @Override
  public List<Long> findSubscribedMemberIds(SubscriptionTargetType targetType, Long targetId) {
    return jpaRepository.findMemberIdsByTargetTypeAndTargetId(targetType, targetId);
  }
}
