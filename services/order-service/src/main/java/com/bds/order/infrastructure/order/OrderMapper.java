package com.bds.order.infrastructure.order;

import com.bds.order.domain.order.Order;
import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.infrastructure.orderReward.OrderRewardJpaEntity;
import com.bds.order.infrastructure.orderReward.OrderRewardMapper;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OrderMapper {

    private final OrderRewardMapper orderRewardMapper;
    private final RewardJpaRepository rewardJpaRepository;

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
                entity.getCancelledAt(),
                entity.getExpiresAt()
        );
    }

    public OrderJpaEntity toJpaEntity(Order domain) {
        OrderJpaEntity entity = OrderJpaEntity.builder()
                .id(domain.getId())
                .orderNo(domain.getOrderNo())
                .memberId(domain.getMemberId())
                .status(domain.getStatus())
                .totalRewardAmount(domain.getTotalRewardAmount())
                .totalShippingCharge(domain.getTotalShippingCharge())
                .cancelReason(domain.getCancelReason())
                .cancelledAt(domain.getCancelledAt())
                .expiresAt(domain.getExpiresAt())
                .build();

        if (domain.getOrderRewards() != null && !domain.getOrderRewards().isEmpty()) {
            List<OrderRewardJpaEntity> rewardEntities = domain.getOrderRewards().stream()
                    .map(orw -> new OrderRewardJpaEntity(
                            orw.getId(),
                            entity,
                            rewardJpaRepository.getReferenceById(orw.getRewardId()),
                            orw.getQty(),
                            orw.getAmount(),
                            orw.getShippingCharge()
                    ))
                    .toList();
            entity.getOrderRewards().addAll(rewardEntities);
        }

        return entity;
    }
}