package com.bds.order.infrastructure.reward;


import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class RewardMapperUnitTest {

    private final RewardMapper rewardMapper = new RewardMapper();

    @Nested
    @DisplayName("JpaEntity에서 도메인으로 변환")
    class ToDomainTest {

        @Test
        void 정상적으로_도메인_객체로_변환한다() {
            LocalDateTime now = LocalDateTime.now();
            FundingJpaEntity funding = new FundingJpaEntity(
                    1L, "펀딩", 100L, null,
                    now, now.plusDays(30), now.plusDays(60),
                    0, 1000000L, 0L, false, new ArrayList<>()
            );
            RewardJpaEntity entity = new RewardJpaEntity(
                    1L, funding, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L
            );

            Reward reward = rewardMapper.toDomain(entity);

            assertThat(reward.getId()).isEqualTo(1L);
            assertThat(reward.getFundingId()).isEqualTo(1L);
            assertThat(reward.getName()).isEqualTo("리워드A");
            assertThat(reward.getDescription()).isEqualTo("설명");
            assertThat(reward.getLimitQty()).isEqualTo(100);
            assertThat(reward.getRemainQty()).isEqualTo(50);
            assertThat(reward.getBadgeType()).isEqualTo(BadgeType.ULTRA_EARLY_BIRD);
            assertThat(reward.getPrice()).isEqualTo(10000L);
            assertThat(reward.getShippingCharge()).isEqualTo(3000L);
        }
    }
}