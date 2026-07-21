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
}