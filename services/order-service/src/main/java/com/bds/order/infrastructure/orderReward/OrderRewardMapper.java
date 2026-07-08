package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.orderReward.OrderReward;
import org.springframework.stereotype.Component;

@Component
public class OrderRewardMapper {

    public OrderReward toDomain(OrderRewardJpaEntity entity) {
        return OrderReward.reconstitute(
                entity.getId(),
                entity.getOrderId(),
                entity.getRewardId(),
                entity.getQty(),
                entity.getShippingCharge(),
                entity.getAmount()
        );
    }
}