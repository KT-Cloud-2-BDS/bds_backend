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
    NotificationSubscriptionEntity entity = toEntity(subscription);
    return toDomain(jpaRepository.save(entity));
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
        .map(this::toDomain);
  }

  @Override
  public List<Long> findSubscribedMemberIds(SubscriptionTargetType targetType, Long targetId) {
    return jpaRepository.findMemberIdsByTargetTypeAndTargetId(targetType, targetId);
  }

  private NotificationSubscriptionEntity toEntity(NotificationSubscription model) {
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

  private NotificationSubscription toDomain(NotificationSubscriptionEntity entity) {
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
