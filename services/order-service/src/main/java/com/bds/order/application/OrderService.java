package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.order.Order;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.BillingResponseDto;
import com.bds.order.presentation.dto.BillingResponseDto.RewardDto;
import com.bds.order.presentation.dto.OrderResponseDto;
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

    public List<OrderResponseDto> getAllOrders(Long memberId, Pageable pageable) {
        List<Order> orderList = orderRepository.findAllByMemberId(memberId, pageable);

        return orderList.stream()
                .map(order -> OrderResponseDto.from(order)).toList();
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

        List<RewardDto> rewardDtos = new ArrayList<>();

        for (Reward reward : foundRewards) {
            Integer qty = rewardIdQtyMap.get(reward.getId());

            if (!reward.isStockSufficient(qty)) {
                throw new BusinessException(ErrorCode.REWARD_STOCK_INSUFFICIENT);
            }

            RewardDto dto = RewardDto.from(reward, qty);
            rewardAmount += dto.amount();
            totalShippingCharge += dto.shippingCharge();
            rewardDtos.add(dto);
        }

        return new BillingResponseDto(memberId, rewardDtos, rewardAmount, totalShippingCharge, rewardAmount + totalShippingCharge);
    }
}
