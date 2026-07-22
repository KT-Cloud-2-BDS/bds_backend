package com.bds.notification.application.dto;

public record ChatPushNotificationDto(
    String title,
    String content,
    Long roomId
) {

}
