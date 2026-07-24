package com.bds.order.presentation.dto;

import com.bds.order.domain.order.OrderStatus;
import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;

import java.time.LocalDateTime;
import java.util.List;

public record OrderDetailResponseDto(
        Long memberId,
        String orderNo,
        OrderStatus orderStatus,
        Long fundingId,
        LocalDateTime fundingDate,
        String title,
        Long hostId,
        boolean isEnded,
        LocalDateTime updatedAt,
        Boolean isFundingSucceeded,
        List<RewardItemDto> rewards,
        Long rewardAmount,
        Long totalShippingCharge,
        Long totalBillingAmount,
        LocalDateTime canceledAt,
        LocalDateTime expiresAt
) {
    public static OrderDetailResponseDto from(Long memberId, OrderDetailProjection order, List<OrderRewardDetailProjection> orderRewards) {
        return new OrderDetailResponseDto(
                memberId,
                order.orderNo(),
                order.status(),
                order.fundingId(),
                order.createdAt(),
                order.fundingTitle(),
                order.hostId(),
                LocalDateTime.now().isAfter(order.holdTo()),
                order.updatedAt(),
                order.isSuccess(),
                orderRewards.stream().map(RewardItemDto::from).toList(),
                order.totalRewardAmount(),
                order.totalShippingCharge(),
                order.totalRewardAmount() + order.totalShippingCharge(),
                order.cancelledAt(),
                order.expiresAt()
        );
    }
}