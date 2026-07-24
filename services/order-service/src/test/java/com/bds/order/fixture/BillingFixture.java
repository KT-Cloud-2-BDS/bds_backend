package com.bds.order.fixture;

import com.bds.order.presentation.dto.BillingRequestDto;
import com.bds.order.presentation.dto.RewardQuantityDto;

import java.util.List;

public class BillingFixture {

    // varargs로 RewardQuantityDto 직접 받기
    public static BillingRequestDto createBillingRequest(Long fundingId, RewardQuantityDto... rewards) {
        return new BillingRequestDto(fundingId, List.of(rewards));
    }

    // RewardQuantityDto 생성 헬퍼
    public static RewardQuantityDto rq(Long rewardId, int quantity) {
        return new RewardQuantityDto(rewardId, quantity);
    }
}