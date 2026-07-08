package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.infrastructure.orderReward.OrderRewardMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderRewardMapper orderRewardMapper;

    public Order toDomain(OrderJpaEntity entity) {
        List<OrderReward> orderRewards = entity.getOrderRewards().stream()
                .map(orderRewardMapper::toDomain)
                .toList();

        return Order.reconstitute(
                entity.getId(),
                entity.getOrderNo(),
                entity.getMemberId(),
                entity.getStatus(),
                entity.getTotalRewardAmount(),
                entity.getTotalShippingCharge(),
                orderRewards,
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