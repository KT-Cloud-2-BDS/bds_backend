package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public Order toDomain(OrderJpaEntity entity) {
        return Order.reconstitute(
                entity.getId(),
                entity.getOrderNo(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getTotalRewardAmount(),
                entity.getTotalShippingCharge(),
                entity.getCancelReason(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCancelledAt()
        );
    }

    public OrderJpaEntity toJpaEntity(Order domain) {
        return OrderJpaEntity.builder()
                .id(domain.getId())
                .memberId(domain.getMemberId())
                .status(domain.getStatus())
                .totalRewardAmount(domain.getTotalRewardAmount())
                .totalShippingCharge(domain.getTotalShippingCharge())
                .cancelReason(domain.getCancelReason())
                .cancelledAt(domain.getCancelledAt())
                .build();
    }
}