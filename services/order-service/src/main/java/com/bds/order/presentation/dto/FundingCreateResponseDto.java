package com.bds.order.presentation.dto;

import com.bds.order.domain.funding.Funding;

import java.time.LocalDateTime;

public record FundingCreateResponseDto(
        Long fundingId,
        String title,
        String status,
        String type,
        LocalDateTime startAt,
        LocalDateTime holdTo,
        LocalDateTime createdAt
) {

    public static FundingCreateResponseDto from(Funding funding) {
        return new FundingCreateResponseDto(
                funding.getId(), funding.getTitle(),
                funding.getStatus().name(), funding.getType().name(), funding.getStartAt(),
                funding.getHoldTo(), funding.getCreatedAt()
        );
    }
}
