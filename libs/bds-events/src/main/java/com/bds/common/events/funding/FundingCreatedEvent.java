package com.bds.common.events.funding;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("funding.exchange::funding.status")
public record FundingCreatedEvent(
        UUID eventId,
        FundingType type,
        String targetType,
        Long targetId,
        Long creatorId
) {
    public static FundingCreatedEvent of(FundingType type, String targetType, Long targetId, Long creatorId) {
        return new FundingCreatedEvent(UUID.randomUUID(), type, targetType, targetId, creatorId);
    }
}
