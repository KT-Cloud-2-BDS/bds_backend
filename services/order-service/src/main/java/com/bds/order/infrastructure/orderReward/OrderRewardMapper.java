package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.orderReward.OrderReward;
import org.springframework.stereotype.Component;

@Component
public class OrderRewardMapper {

    public OrderReward toDomain(OrderRewardJpaEntity entity) {
        return OrderReward.of(
                entity.getId(),
                entity.getOrder().getId(),
                entity.getReward().getId(),
                entity.getQty()
        );
    }
}