package com.bds.notification.application;

import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.Notification;
import com.bds.notification.domain.notification.entity.NotificationSubscription;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.repository.NotificationRepository;
import com.bds.notification.domain.notification.repository.NotificationSubscriptionRepository;
import com.bds.notification.infrastructure.sse.SseEmitterManager;
import com.bds.notification.presentation.dto.NotificationListResponseDto;
import com.bds.notification.presentation.dto.NotificationResponseDto;
import com.bds.notification.presentation.dto.NotificationSubscribeResponseDto;
import com.bds.notification.presentation.dto.UnreadCountResponseDto;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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
  public NotificationListResponseDto getNotifications(Long memberId, Pageable pageable) {
    Page<Notification> notifications = notificationRepository.findByMemberIdOrderByCreatedAtDesc(
        memberId, pageable);

    long unReadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);

    List<NotificationResponseDto> responses = notifications.getContent().stream()
        .map(NotificationResponseDto::from)
        .toList();

    notificationRepository.markAllAsReadByMemberId(memberId);

    return NotificationListResponseDto.of(responses, notifications.getTotalElements(), unReadCount);
  }

  // 읽지 않은 알림 반환
  public UnreadCountResponseDto getUnreadCount(Long memberId) {

    long unreadCount = notificationRepository.countByMemberIdAndIsReadFalse(memberId);
    return new UnreadCountResponseDto(unreadCount);
  }

  // 알림 등록
  @Transactional
  public NotificationSubscribeResponseDto subscribe(Long memberId,
      SubscriptionTargetType targetType,
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
    try {
      notificationSubscriptionRepository.save(notificationSubscription);
    } catch (DataIntegrityViolationException e) {
      throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
    }

    return new NotificationSubscribeResponseDto(targetType, targetId, true);
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
