package com.bds.notification.presentation.dto;

import com.bds.notification.domain.notification.entity.SubscriptionTargetType;

public record NotificationSubscribeResponseDto(
    SubscriptionTargetType targetType,
    Long targetId,
    Boolean subscribed
) {

}
