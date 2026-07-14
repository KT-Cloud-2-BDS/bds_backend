package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import org.springframework.stereotype.Component;

@Component
public class FundingMapper {

    public Funding toDomain(FundingJpaEntity entity) {
        return Funding.of(
                entity.getId(),
                entity.getTitle(),
                entity.getCreatorId(),
                entity.getStatus(),
                entity.getStartAt(),
                entity.getHoldTo(),
                entity.getPayAt(),
                entity.getParticipationCnt(),
                entity.getGoalAmount(),
                entity.getCurrentAmount(),
                entity.getIsSuccess(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public FundingJpaEntity toJpaEntity(Funding entity) {
        return FundingJpaEntity.builder()
                .id(entity.getId())
                .title(entity.getTitle())
                .creatorId(entity.getCreatorId())
                .status(entity.getStatus())
                .startAt(entity.getStartAt())
                .holdTo(entity.getHoldTo())
                .payAt(entity.getPayAt())
                .participationCnt(entity.getParticipationCnt())
                .goalAmount(entity.getGoalAmount())
                .currentAmount(entity.getCurrentAmount())
                .isSuccess(entity.getIsSuccess())
                .build();
    }
}