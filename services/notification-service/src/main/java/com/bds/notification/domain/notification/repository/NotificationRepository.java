package com.bds.notification.domain.notification.repository;

import com.bds.notification.domain.notification.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

  Notification save(Notification notification);

  Page<Notification> findByMemberId(Long memberId, Pageable pageable);

  long countUnreadByMemberId(Long memberId);

  int markAllAsRead(Long memberId);

}
