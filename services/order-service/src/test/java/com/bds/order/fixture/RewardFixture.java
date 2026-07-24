package com.bds.order.fixture;

import com.bds.order.domain.reward.Reward;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaEntity;

import java.time.LocalDateTime;

public class RewardFixture {

    private final static LocalDateTime OFFER_AT = LocalDateTime.now().plusDays(60);
    private final static Long REWARD_PRICE = 10000L;

    public static Reward createReward(Long id, Long fundingId, Long price) {
        return Reward.of(id, fundingId, "리워드A", "설명A", 100, 10,
                null, price, OFFER_AT, 3000L);
    }

    public static Reward createReward(Long id, Long fundingId, Long price, Long shippingCharge) {
        return Reward.of(id, fundingId, "리워드A", "설명A", 100, 10,
                null, price, OFFER_AT, shippingCharge);
    }

    public static RewardJpaEntity createRewardJpaEntity(FundingJpaEntity funding) {
        return new RewardJpaEntity(
                null,
                funding,
                "리워드A",
                "설명A",
                100,
                10,
                null,
                REWARD_PRICE,
                OFFER_AT,
                3000L
        );
    }

    public static RewardJpaEntity createRewardJpaEntity(FundingJpaEntity funding, Long price, Long shippingCharge) {
        return new RewardJpaEntity(
                null,
                funding,
                "리워드A",
                "설명A",
                100,
                10,
                null,
                price,
                OFFER_AT,
                shippingCharge
        );
    }

    public static RewardJpaEntity createRewardJpaEntityWithStock(FundingJpaEntity funding, int stock) {
        return new RewardJpaEntity(
                null,
                funding,
                "리워드A",
                "설명A",
                100,
                stock,
                null,
                REWARD_PRICE,
                OFFER_AT,
                3000L
        );
    }

}
