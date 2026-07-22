package com.bds.notification.application.event;

import com.bds.notification.application.dto.PushNotificationDto;

public record NotificationCreatedEvent(
    Long memberId,
    PushNotificationDto payload
) {

}
