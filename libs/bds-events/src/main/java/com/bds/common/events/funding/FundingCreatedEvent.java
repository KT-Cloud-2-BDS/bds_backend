package com.bds.common.events.funding;

import org.springframework.modulith.events.Externalized;

import java.util.UUID;

@Externalized("funding.exchange::funding.created")
public record FundingCreatedEvent(
        UUID eventId,
        Long creatorId,
        Long productId
) {
    public static FundingCreatedEvent of(Long creatorId, Long productId) {
        return new FundingCreatedEvent(UUID.randomUUID(), creatorId, productId);
    }
}
