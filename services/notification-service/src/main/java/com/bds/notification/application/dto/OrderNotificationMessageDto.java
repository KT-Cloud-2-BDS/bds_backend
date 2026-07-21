package com.bds.notification.application.dto;

import com.bds.notification.domain.notification.entity.NotificationType;

public record OrderNotificationMessageDto(
    NotificationType type,
    Long memberId,
    String fundingTitle,
    String orderNo
) {

}
