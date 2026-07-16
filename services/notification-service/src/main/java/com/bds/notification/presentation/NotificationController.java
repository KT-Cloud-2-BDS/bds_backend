package com.bds.notification.presentation;

import com.bds.common.annotation.LoginUser;
import com.bds.common.dto.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @GetMapping(value = "/connect", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter connect(@LoginUser CurrentUser currentUser) {
    return notificationService.connect(currentUser.id());
  }

  @GetMapping("")
  public NotificationListResponseDto notifications(
      @LoginUser CurrentUser currentUser,
      @PageableDefault(size = 20) Pageable pageable) {
    return notificationService.getNotifications(currentUser.id(), pageable);
  }

  @GetMapping("/unread-count")
  public UnreadCountResponseDto unreadCount(@LoginUser CurrentUser currentUser) {
    return notificationService.getUnreadCount(currentUser.id());
  }

  @PostMapping("/subscriptions/{targetType}/{targetId}")
  public NotificationSubscribeResponseDto subscribe(
      @LoginUser CurrentUser currentUser,
      @PathVariable String targetType,
      @PathVariable Long targetId
  ) {
    SubscriptionTargetType type;
    try {
      type = SubscriptionTargetType.valueOf(targetType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_TARGET_TYPE);
    }
    return notificationService.subscribe(currentUser.id(), type, targetId);
  }

  @DeleteMapping("/subscriptions/{targetType}/{targetId}")
  public ResponseEntity<Void> unsubscribe(
      @LoginUser CurrentUser currentUser,
      @PathVariable String targetType,
      @PathVariable Long targetId
  ) {
    SubscriptionTargetType type;
    try {
      type = SubscriptionTargetType.valueOf(targetType.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BusinessException(ErrorCode.INVALID_TARGET_TYPE);
    }

    notificationService.unsubscribe(currentUser.id(), type, targetId);
    return ResponseEntity.noContent().build();
  }

}
