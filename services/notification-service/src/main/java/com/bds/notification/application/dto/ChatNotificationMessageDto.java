package com.bds.notification.application.dto;

public record ChatNotificationMessageDto(
    Long receiverId,
    Long roomId,
    String content
) {

}
