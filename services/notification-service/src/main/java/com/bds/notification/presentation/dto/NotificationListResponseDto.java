package com.bds.notification.presentation.dto;

import java.util.List;

public record NotificationListResponseDto(
    List<NotificationResponseDto> notifications,
    long totalCount,
    long unreadCount
) {

  public static NotificationListResponseDto of(List<NotificationResponseDto> notifications,
      long totalCount, long unreadCount) {
    return new NotificationListResponseDto(notifications, totalCount, unreadCount);
  }
}
