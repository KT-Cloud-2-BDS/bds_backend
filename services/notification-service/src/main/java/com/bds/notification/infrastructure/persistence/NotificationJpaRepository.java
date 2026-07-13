package com.bds.notification.infrastructure.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationJpaRepository extends JpaRepository<NotificationEntity, Long> {

  Page<NotificationEntity> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

  long countByMemberIdAndIsReadFalse(Long memberId);

  @Modifying
  @Query("UPDATE NotificationEntity n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.memberId = :memberId AND n.isRead = false")
  int markAllAsReadByMemberId(Long memberId);
}
