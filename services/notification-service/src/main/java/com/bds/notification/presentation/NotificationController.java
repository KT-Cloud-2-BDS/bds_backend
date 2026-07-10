package com.bds.notification.presentation;

import com.bds.notification.application.NotificationService;
import com.bds.notification.common.exception.BusinessException;
import com.bds.notification.common.exception.ErrorCode;
import com.bds.notification.domain.notification.entity.SubscriptionTargetType;
import com.bds.notification.presentation.dto.NotificationListResponseDto;
import com.bds.notification.presentation.dto.NotificationSubscribeResponseDto;
import com.bds.notification.presentation.dto.UnreadCountResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(@RequestHeader("X-User-Id") Long memberId) {
    return notificationService.connect(memberId);
  }

  @GetMapping("")
  public NotificationListResponseDto notifications(
      @RequestHeader("X-User-Id") Long memberId,
      @PageableDefault(size = 20) Pageable pageable) {
    return notificationService.getNotifications(memberId, pageable);
  }

  @GetMapping("/unread-count")
  public UnreadCountResponseDto unreadCount(@RequestHeader("X-User-Id") Long memberId) {
    return notificationService.getUnreadCount(memberId);
  }

  @PostMapping("/subscriptions/{targetType}/{targetId}")
  public NotificationSubscribeResponseDto subscribe(
      @RequestHeader("X-User-Id") Long memberId,
      @PathVariable String targetType,
      @PathVariable Long targetId
  ) {
    SubscriptionTargetType type;
    try {
      type = SubscriptionTargetType.valueOf(targetType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_TARGET_TYPE);
    }
    return notificationService.subscribe(memberId, type, targetId);
  }

  @DeleteMapping("/subscriptions/{targetType}/{targetId}")
  public ResponseEntity<Void> unsubscribe(
      @RequestHeader("X-User-Id") Long memberId,
      @PathVariable String targetType,
      @PathVariable Long targetId
  ) {
    SubscriptionTargetType type;
    try {
      type = SubscriptionTargetType.valueOf(targetType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_TARGET_TYPE);
    }

    notificationService.unsubscribe(memberId, type, targetId);
    return ResponseEntity.noContent().build();
  }

}
