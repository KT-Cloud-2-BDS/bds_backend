package com.bds.order.presentation.dto;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.reward.Reward;

import java.time.LocalDateTime;
import java.util.List;

public record FundingDetailResponseDto(
        Long id,
        String title,
        Long creatorId,
        String status,
        Long goalAmount,
        Long currentAmount,
        int participationCnt,
        LocalDateTime startAt,
        LocalDateTime holdTo,
        LocalDateTime payAt,
        Boolean isSuccess,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<RewardDto> rewards
) {
    public static FundingDetailResponseDto from(Funding funding, List<Reward> rewards) {
        List<RewardDto> rewardDtos = rewards.stream()
                .map(RewardDto::from)
                .toList();

        return new FundingDetailResponseDto(
                funding.getId(), funding.getTitle(), funding.getCreatorId(),
                funding.getStatus().name(), funding.getGoalAmount(), funding.getCurrentAmount(),
                funding.getParticipationCnt(), funding.getStartAt(), funding.getHoldTo(),
                funding.getPayAt(), funding.getIsSuccess(),
                funding.getCreatedAt(), funding.getUpdatedAt(),
                rewardDtos
        );
    }

    public record RewardDto(
            Long id,
            String name,
            String description,
            int limitQty,
            int remainQty,
            String badgeType,
            Long price,
            LocalDateTime offerAt,
            Long shippingCharge
    ) {
        public static RewardDto from(Reward reward) {
            return new RewardDto(
                    reward.getId(), reward.getName(), reward.getDescription(),
                    reward.getLimitQty(), reward.getRemainQty(),
                    reward.getBadgeType() != null ? reward.getBadgeType().name() : null,
                    reward.getPrice(), reward.getOfferAt(), reward.getShippingCharge()
            );
        }
    }
}
