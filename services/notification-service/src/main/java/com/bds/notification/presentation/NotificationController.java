package com.bds.notification.presentation;

import com.bds.notification.application.NotificationService;
import com.bds.notification.presentation.dto.NotificationListResponse;
import com.bds.notification.presentation.dto.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
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
  public NotificationListResponse notifications(
      @RequestHeader("X-User-Id") Long memberId,
      @PageableDefault(size = 20) Pageable pageable) {
    return notificationService.getNotifications(memberId, pageable);
  }

  @GetMapping("/unread-count")
  public UnreadCountResponse unreadCount(@RequestHeader("X-User-Id") Long memberId) {
    return notificationService.getUnreadCount(memberId);
  }

}
