package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.order.CancelReason;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.domain.orderReward.OrderReward;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
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

    @Transactional
    public BillingResponseDto createBilling(Long memberId, BillingRequestDto reqDto) {

        Long fundingId = validateFunding(reqDto.fundingId());

        ValidatedRewards validatedRewards = validateRewards(fundingId, reqDto.rewards());

        Long rewardAmount = 0L;
        Long totalShippingCharge = 0L;

        List<RewardItemDto> RewardItemDtos = new ArrayList<>();

        List<OrderReward> orderRewardList = new ArrayList<>();

        for (Reward reward : validatedRewards.rewards) {
            int qty = validatedRewards.qtyMap.get(reward.getId());

            if (!reward.isStockSufficient(qty)) {
                throw new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT);
            }

            RewardItemDto dto = RewardItemDto.from(reward, qty);
            rewardAmount += dto.amount();
            totalShippingCharge += dto.shippingCharge();
            RewardItemDtos.add(dto);
            orderRewardList.add(OrderReward.of(dto, null));
        }

        Order order = Order.create(memberId, rewardAmount, totalShippingCharge,
                reqDto.isReservedOrder() ? OrderStatus.RESERVED : OrderStatus.PENDING);
        order.saveOrderRewards(orderRewardList);

        Order savedOrder = orderRepository.save(order);
        return BillingResponseDto.from(savedOrder, RewardItemDtos);
    }

    @Transactional
    public OrderCreateResponseDto createOrder(Long memberId, OrderCreateRequestDto reqDto) {

        validateFunding(reqDto.fundingId());

        Order order = orderRepository.findByIdForUpdate(reqDto.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!memberId.equals(order.getMemberId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        try {
            order.startPayment();
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED, e.getMessage());
        }

        order.getOrderRewards().forEach(orw -> {
            if (rewardRepository.decreaseStock(orw.getRewardId(), orw.getQty()) == 0) {
                throw new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT);
            }
        });


        // TODO: Calling Payment API 결제 시작
        orderRepository.save(order);
        return new OrderCreateResponseDto(memberId, order.getOrderNo(), order.getTotalAmount(), order.getStatus(), LocalDateTime.now());
    }

    @Transactional
    public OrderCancelResponseDto cancelOrder(Long memberId, Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        if (!memberId.equals(order.getMemberId())) {
            throw new BusinessException(ErrorCode.ORDER_ACCESS_DENIED);
        }

        try {
            order.cancelOrder(CancelReason.USER_CANCEL.name());
        } catch (IllegalStateException e) {
            throw new BusinessException(ErrorCode.ORDER_STATUS_CHANGE_NOT_ALLOWED, e.getMessage());
        }

        order.getOrderRewards().forEach(orw -> {
            rewardRepository.increaseRemainQty(orw.getRewardId(), orw.getQty());
        });

        // TODO: Calling Refund API
        orderRepository.save(order);
        return new OrderCancelResponseDto(order.getOrderNo(), order.getStatus(), order.getCancelledAt(), "REFUND_REQUESTED");
    }

    private Long validateFunding(Long fundingId) {
        Funding funding = fundingRepository.findById(fundingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.FUNDING_NOT_FOUND));

        if (!funding.isFundingPeriod(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.FUNDING_NOT_AVAILABLE);
        }

        return funding.getId();
    }

    private ValidatedRewards validateRewards(Long fundingId, List<RewardQuantityDto> rewards) {
        Map<Long, Integer> rewardIdQtyMap = rewards.stream()
                .collect(Collectors.toMap(
                        RewardQuantityDto::id,
                        RewardQuantityDto::qty,
                        (existing, _) -> {
                            throw new BusinessException(ErrorCode.REWARD_DUPLICATED);
                        }
                ));

        List<Reward> foundRewards = rewardRepository.findAllByIdAndFundingId(
                rewardIdQtyMap.keySet().stream().toList(), fundingId);

        if (foundRewards.size() != rewards.size()) {
            throw new BusinessException(ErrorCode.REWARD_NOT_FOUND);
        }

        return new ValidatedRewards(foundRewards, rewardIdQtyMap);
    }

    @Transactional
    public void processStatusUpdate(Long orderId, OrderStatus targetStatus) {
        findOrderForUpdate(orderId).ifPresent(order -> {
            try {
                order.updateStatus(targetStatus);
                orderRepository.save(order);
            } catch (IllegalStateException e) {
                log.warn("Status transition failed: orderId={}, target={}, reason={}",
                        orderId, targetStatus, e.getMessage());
            }
        });
    }

    @Transactional
    public void processCancelledUpdate(Long orderId, String cancelReason) {
        findOrderForUpdate(orderId).ifPresent(order -> {
            try {
                order.cancelOrder(cancelReason);
                order.getOrderRewards().forEach(orw ->
                        rewardRepository.increaseRemainQty(orw.getRewardId(), orw.getQty())
                );
                orderRepository.save(order);
            } catch (IllegalStateException e) {
                log.warn("Cancel failed: orderId={}, reason={}", orderId, e.getMessage());
            }
        });
    }

    @Transactional
    public void publishSettlement(Long orderId) {
        // 상태 변경 없음, 발행만
        // TODO: Outbox에 정산 요청 메시지 저장 (type: SETTLEMENT)
    }

    @Transactional
    public void processPayingAndPublishSettlement(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_JUDGE] Order not found: orderId=" + orderId));

        order.updateStatus(OrderStatus.PAYING);
        orderRepository.save(order);

        // TODO: Outbox에 결제 요청 메시지 저장 (PAY 측 스펙 확정 후)
        // outboxRepository.save(OutboxMessage.create(...))
    }

    @Transactional
    public void processCancelAndPublishRefund(Long orderId) {
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new IllegalStateException(
                        "[FUNDING_JUDGE] Order not found: orderId=" + orderId));

        order.cancelOrder(CancelReason.FUNDING_FAILED.name());
        order.getOrderRewards().forEach(orw ->
                rewardRepository.increaseRemainQty(orw.getRewardId(), orw.getQty())
        );
        orderRepository.save(order);

        // TODO: Outbox에 환불 요청 메시지 저장 (PAY 측 스펙 확정 후)
    }

    private Optional<Order> findOrderForUpdate(Long orderId) {
        Optional<Order> orderOpt = orderRepository.findByIdForUpdate(orderId);
        if (orderOpt.isEmpty()) {
            log.warn("Order not found: orderId={}", orderId);
        }
        return orderOpt;
    }

    private record ValidatedRewards(List<Reward> rewards, Map<Long, Integer> qtyMap) {
    }

}
