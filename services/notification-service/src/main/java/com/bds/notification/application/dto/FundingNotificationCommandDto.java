package com.bds.notification.application.dto;

public record FundingNotificationCommandDto(
    String type,
    String targetType,
    Long targetId
) {

}
