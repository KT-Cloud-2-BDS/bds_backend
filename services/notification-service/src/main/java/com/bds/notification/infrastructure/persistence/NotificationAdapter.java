package com.bds.notification.infrastructure.persistence;

import com.bds.notification.domain.notification.model.Notification;
import com.bds.notification.domain.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class NotificationAdapter implements NotificationRepository {

  private final NotificationJpaRepository jpaRepository;

  @Override
  public Page<Notification> findByMemberId(Long memberId, Pageable pageable) {
    return jpaRepository.findByMemberIdOrderByCreatedAtDescNotificationIdDesc(memberId, pageable)
        .map(NotificationMapper::toDomain);
  }

  @Override
  public long countUnreadByMemberId(Long memberId) {
    return jpaRepository.countByMemberIdAndIsReadFalse(memberId);
  }

  @Override
  public int markAllAsRead(Long memberId) {
    return jpaRepository.markAllAsReadByMemberId(memberId);
  }
}
