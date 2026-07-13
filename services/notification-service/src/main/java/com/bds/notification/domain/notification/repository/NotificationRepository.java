package com.bds.notification.domain.notification.repository;

import com.bds.notification.domain.notification.model.Notification;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NotificationRepository {

  Page<Notification> findByMemberId(Long memberId, Pageable pageable);

  long countUnreadByMemberId(Long memberId);

  int markAllAsRead(Long memberId);

}
