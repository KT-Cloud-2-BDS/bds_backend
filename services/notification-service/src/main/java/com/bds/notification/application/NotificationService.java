package com.bds.notification.application;

import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.Notification;
import com.bds.notification.domain.notification.entity.NotificationSubscription;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.repository.NotificationRepository;
import com.bds.notification.domain.notification.repository.NotificationSubscriptionRepository;
import com.bds.notification.infrastructure.sse.SseEmitterManager;
import com.bds.notification.presentation.dto.NotificationListResponse;
import com.bds.notification.presentation.dto.NotificationResponse;
import com.bds.notification.presentation.dto.NotificationSubscribeResponse;
import com.bds.notification.presentation.dto.UnreadCountResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
@RequiredArgsConstructor
public class NotificationService {

  private final SseEmitterManager sseEmitterManager;
  private final NotificationRepository notificationRepository;
  private final NotificationSubscriptionRepository notificationSubscriptionRepository;

  // 생성한 SSE Emitter 반환
  public SseEmitter connect(Long memberId) {
    SseEmitter emitter = sseEmitterManager.create(memberId);

    try {
      emitter.send(
          SseEmitter.event()
              .name("connect")
              .data("connected")
      );
    } catch (IOException e) {
      sseEmitterManager.remove(memberId);
      throw new BusinessException(ErrorCode.SSE_SEND_FAILED);
    }

    return emitter;
  }

  // 알림 리스트 반환
  @Transactional
  public NotificationListResponse getNotifications(Long memberId, Pageable pageable) {
    List<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
        memberId, pageable);

    long unReadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

    List<NotificationResponse> responses = notifications.stream()
        .map(NotificationResponse::from)
        .toList();

    notificationRepository.markAllAsReadByMemberId(memberId);

    return NotificationListResponse.of(responses, notifications.size(), unReadCount);
  }

  // 읽지 않은 알림 반환
  public UnreadCountResponse getUnreadCount(Long memberId) {

    long unreadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    return new UnreadCountResponse(unreadCount);
  }

  // 알림 등록
  @Transactional
  public NotificationSubscribeResponse subscribe(Long memberId, SubscriptionTargetType targetType,
      Long targetId) {
    if (notificationSubscriptionRepository.existsByMemberIdAndTargetTypeAndTargetId(memberId,
        targetType, targetId)) {
      throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
    }
    NotificationSubscription notificationSubscription = NotificationSubscription.builder()
        .targetType(targetType)
        .memberId(memberId)
        .targetId(targetId)
        .build();

    notificationSubscriptionRepository.save(notificationSubscription);

    return new NotificationSubscribeResponse(targetType, targetId, true);
  }

  // 알림 해지
  @Transactional
  public void unsubscribe(Long memberId, SubscriptionTargetType targetType,
      Long targetId) {
    NotificationSubscription subscription = notificationSubscriptionRepository.findByMemberIdAndTargetTypeAndTargetId(
            memberId, targetType, targetId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    subscription.softDelete();
  }

}
