package com.bds.notification.application;

import com.bds.notification.application.dto.FundingNotificationCommandDto;
import com.bds.notification.application.dto.OrderNotificationMessageDto;
import com.bds.notification.application.dto.PushNotificationDto;
import com.bds.notification.application.event.NotificationCreatedEvent;
import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.NotificationChannel;
import com.bds.notification.domain.notification.entity.NotificationType;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.domain.notification.model.Notification;
import com.bds.notification.domain.notification.model.NotificationSubscription;
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
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher eventPublisher;

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
    Page<Notification> notifications = notificationRepository.findByMemberId(memberId, pageable);

    long unReadCount = notificationRepository.countUnreadByMemberId(memberId);

    List<NotificationResponseDto> responses = notifications.getContent().stream()
        .map(NotificationResponseDto::from)
        .toList();

    notificationRepository.markAllAsRead(memberId);

    return NotificationListResponseDto.of(responses, notifications.getTotalElements(), unReadCount);
  }

  // 읽지 않은 알림 반환
  public UnreadCountResponseDto getUnreadCount(Long memberId) {

    long unreadCount = notificationRepository.countUnreadByMemberId(memberId);
    return new UnreadCountResponseDto(unreadCount);
  }

  // 알림 등록
  @Transactional
  public NotificationSubscribeResponseDto subscribe(Long memberId,
      SubscriptionTargetType targetType,
      Long targetId) {
    if (notificationSubscriptionRepository.existsActiveSubscription(memberId, targetType,
        targetId)) {
      throw new BusinessException(ErrorCode.SUBSCRIPTION_ALREADY_EXISTS);
    }
    NotificationSubscription notificationSubscription = NotificationSubscription.create(memberId,
        targetType, targetId);
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
    NotificationSubscription subscription = notificationSubscriptionRepository.findActiveSubscription(
            memberId, targetType, targetId)
        .orElseThrow(() -> new BusinessException(ErrorCode.SUBSCRIPTION_NOT_FOUND));

    subscription.softDelete();
    notificationSubscriptionRepository.save(subscription);
  }

  // 알림 생성
  @Transactional
  public void createNotification(Long memberId, NotificationType type, String targetId,
      String title, String body, NotificationChannel channel) {
    Notification notification = Notification.create(memberId, type, targetId, title, body, channel);

    notificationRepository.save(notification);

    eventPublisher.publishEvent(
        new NotificationCreatedEvent(memberId, PushNotificationDto.from(notification)));
  }

  // 주문 상태 알림 생성
  @Transactional
  public void createOrderNotification(OrderNotificationMessageDto command) {
    String title = switch (command.type()) {
      case PAID -> "주문이 완료됐어요";
      case REFUNDED -> "환불이 완료됐어요";
      default -> throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_TYPE);
    };
    String body = switch (command.type()) {
      case PAID -> "[" + command.fundingTitle() + "] 주문이 완료됐습니다";
      case REFUNDED -> "[" + command.fundingTitle() + "] 환불이 완료됐습니다";
      default -> throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_TYPE);
    };

    createNotification(command.memberId(), command.type(), command.orderNo(), title, body,
        NotificationChannel.SSE);
  }

  public void createFundingNotification(FundingNotificationCommandDto command) {
    List<Long> memberIds = notificationSubscriptionRepository.findSubscribedMemberIds(
        SubscriptionTargetType.valueOf(command.targetType()),
        Long.parseLong(command.targetId())
    );

    String title = switch (command.type()) {
      case FUNDING_START -> "펀딩이 시작되었어요";
      case FUNDING_SUCCESS -> "펀딩에 성공하셨어요";
      case FUNDING_FAIL -> "펀딩에 실패하셨어요";
      default -> throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_TYPE);
    };

    // TODO: TargetName을 구독 시 생성하여 NotificationSubscription에서 읽어와서 각자 생성시킬예정임. 람다 함수 쓰면 되지 않을까
    String body = switch (command.type()) {
      case FUNDING_START -> "[상품 #" + command.targetId() + "] 펀딩이 시작되었습니다.";
      case FUNDING_SUCCESS -> "[상품 #" + command.targetId() + "] 펀딩에 성공하셨습니다.";
      case FUNDING_FAIL -> "[상품 #" + command.targetId() + "] 펀딩에 실패하셨습니다.";
      default -> throw new BusinessException(ErrorCode.INVALID_NOTIFICATION_TYPE);
    };

    memberIds.forEach(memberId -> {
      createNotification(memberId, command.type(), command.targetId(), title, body,
          NotificationChannel.SSE);
    });


  }


}
