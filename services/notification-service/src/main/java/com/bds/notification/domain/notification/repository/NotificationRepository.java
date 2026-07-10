package com.bds.notification.domain.notification.repository;

import com.bds.notification.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

  Page<Notification> findByMemberIdOrderByCreatedAtDesc(Long memberId, Pageable pageable);

  long countByMemberIdAndIsReadFalse(Long memberId);

  @Modifying
  @Query("UPDATE Notification n SET n.isRead = true, n.readAt = CURRENT_TIMESTAMP WHERE n.memberId =  :memberId AND n.isRead = false")
  int markAllAsReadByMemberId(Long memberId);

}
