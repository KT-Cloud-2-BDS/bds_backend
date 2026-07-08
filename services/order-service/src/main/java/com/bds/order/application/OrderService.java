package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.orderReward.OrderRewardRepository;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.infrastructure.order.OrderDetailProjection;
import com.bds.order.infrastructure.order.OrderListProjection;
import com.bds.order.infrastructure.orderReward.OrderRewardDetailProjection;
import com.bds.order.presentation.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final FundingRepository fundingRepository;
    private final RewardRepository rewardRepository;
    private final OrderRewardRepository orderRewardRepository;

    public List<OrderResponseDto> getAllOrders(Long memberId, Pageable pageable) {
        List<OrderListProjection> orderList = orderRepository.findOrderListWithFunding(memberId, pageable);

        return orderList.stream().map(OrderResponseDto::from).toList();
    }

    public OrderDetailResponseDto getOrderDetail(Long memberId, Long orderId) {
        OrderDetailProjection order = orderRepository.findOrderDetailWithFunding(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        List<OrderRewardDetailProjection> orderRewards = orderRewardRepository.findOrderRewardDetailsWithReward(orderId);

        return OrderDetailResponseDto.from(memberId, order, orderRewards);
    }

    public BillingResponseDto createBilling(Long memberId, BillingRequestDto reqDto) {

        Funding funding = fundingRepository.findById(reqDto.fundingId())
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        if (!funding.isFuningPeriod(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FUNDING_NOT_AVAILABLE);
        }

        Map<Long, Integer> rewardIdQtyMap = reqDto.rewards().stream()
                .collect(Collectors.toMap(
                        BillingRequestDto.RewardItemDto::id,
                        BillingRequestDto.RewardItemDto::qty,
                        (existing, _) -> {
                            throw new BusinessException(ErrorCode.REWARD_DUPLICATED, existing);
                        }
                ));

        List<Reward> foundRewards = rewardRepository.findAllByIdAndFundingId(
                rewardIdQtyMap.keySet().stream().toList(), funding.getId());

        if (foundRewards.size() != reqDto.rewards().size()) {
            throw new BusinessException(ErrorCode.REWARD_NOT_FOUND);
        }

        Long rewardAmount = 0L;
        Long totalShippingCharge = 0L;

        List<RewardItemDto> rewardDtos = new ArrayList<>();

        for (Reward reward : foundRewards) {
            Integer qty = rewardIdQtyMap.get(reward.getId());

            if (!reward.isStockSufficient(qty)) {
                throw new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT);
            }

            RewardItemDto dto = RewardItemDto.from(reward, qty);
            rewardAmount += dto.amount();
            totalShippingCharge += dto.shippingCharge();
            rewardDtos.add(dto);
        }

        return new BillingResponseDto(memberId, rewardDtos, rewardAmount, totalShippingCharge, rewardAmount + totalShippingCharge);
    }

    public OrderCancelResponseDto cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!memberId.equals(order.getMemberId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        try {
            order.cancelOrder(CancelReason.USER_CANCEL);
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED, e.getMessage());
        }

        order.getOrderRewards().forEach(or -> {
            rewardRepository.increaseRemainQty(or.getRewardId(), or.getQty());
        });

        // TODO: Calling Refund API
        return new OrderCancelResponseDto(order.getOrderNo(), order.getStatus(), order.getCancelledAt(), "REFUND_REQUESTED");
    }
}
