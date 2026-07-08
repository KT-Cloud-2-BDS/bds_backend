package com.bds.notification.presentation.dto;

import java.util.List;

public record NotificationListResponse(
    List<NotificationResponse> notifications,
    long totalCount,
    long unreadCount
) {

  public static NotificationListResponse of(List<NotificationResponse> notifications,
      long totalCount, long unreadCount) {
    return new NotificationListResponse(notifications, totalCount, unreadCount);
  }
}
