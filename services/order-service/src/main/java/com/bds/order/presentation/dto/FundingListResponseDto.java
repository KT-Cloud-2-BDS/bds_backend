package com.bds.order.presentation.dto;

import com.bds.order.domain.funding.Funding;

import java.time.LocalDateTime;

public record FundingListResponseDto(
        Long id,
        String title,
        Long creatorId,
        String status,
        Long goalAmount,
        Long currentAmount,
        int participationCnt,
        LocalDateTime startAt,
        LocalDateTime holdTo,
        Boolean isSuccess,
        LocalDateTime createdAt
) {

    public static FundingListResponseDto from(Funding funding) {
        return new FundingListResponseDto(
                funding.getId(), funding.getTitle(), funding.getCreatorId(),
                funding.getStatus().name(), funding.getGoalAmount(), funding.getCurrentAmount(),
                funding.getParticipationCnt(), funding.getStartAt(), funding.getHoldTo(),
                funding.getIsSuccess(), funding.getCreatedAt()
        );
    }
}
