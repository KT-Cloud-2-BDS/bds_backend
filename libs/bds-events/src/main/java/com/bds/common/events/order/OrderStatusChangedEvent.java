package com.bds.common.events.order;

import org.springframework.modulith.events.Externalized;

@Externalized("notification.exchange::order.status")
public record OrderStatusChangedEvent(
        String type,
        Long memberId,
        String fundingTitle,
        String orderNo
) {
    public static OrderStatusChangedEvent of(String type, Long memberId, String fundingTitle, String orderNo) {
        return new OrderStatusChangedEvent(type, memberId, fundingTitle, orderNo);
    }
}
