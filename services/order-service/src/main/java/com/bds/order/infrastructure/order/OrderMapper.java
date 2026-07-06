package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toDomain(OrderJpaEntity entity) {
        return Order.of(
                entity.getId(),
                entity.getOrderNo(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getAmount(),
                entity.getCancelReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}