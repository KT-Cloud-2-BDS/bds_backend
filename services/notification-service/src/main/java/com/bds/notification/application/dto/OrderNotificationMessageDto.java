package com.bds.notification.application.dto;

public record OrderNotificationMessageDto(
    String type,
    Long memberId,
    String fundingTitle,
    String orderNo
) {

}
