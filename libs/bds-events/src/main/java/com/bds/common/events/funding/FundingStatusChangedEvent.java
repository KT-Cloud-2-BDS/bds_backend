package com.bds.common.events.funding;

import org.springframework.modulith.events.Externalized;

@Externalized("notification.exchange::funding.status")
public record FundingStatusChangedEvent(
        String type,
        String targetType,
        Long targetId,
        Long creatorId
) {
    public static FundingStatusChangedEvent of(String type, Long targetId, Long creatorId) {
        return new FundingStatusChangedEvent(type, "PRODUCT", targetId, creatorId);
    }
}
