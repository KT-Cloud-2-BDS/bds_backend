package com.bds.notification.application.dto;

import com.bds.notification.domain.notification.entity.NotificationType;

public record FundingNotificationCommandDto(
    NotificationType type,
    String targetId,
    String targetType
) {

}
