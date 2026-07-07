package com.bds.order.infrastructure.reward;

import com.bds.order.domain.reward.Reward;
import org.springframework.stereotype.Component;

@Component
public class RewardMapper {

    public Reward toDomain(RewardJpaEntity entity) {
        return Reward.of(
                entity.getId(),
                entity.getFunding().getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getLimitQty(),
                entity.getRemainQty(),
                entity.getBadgeType(),
                entity.getPrice(),
                entity.getOfferAt(),
                entity.getShippingCharge()
        );
    }
}