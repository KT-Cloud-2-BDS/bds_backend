package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.orderReward.OrderReward;
import com.bds.order.infrastructure.order.OrderJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class OrderRewardMapperUnitTest {

    private final OrderRewardMapper orderRewardMapper = new OrderRewardMapper();

    @Nested
    @DisplayName("JpaEntity에서 도메인으로 변환")
    class ToDomainTest {

        @Test
        void 정상적으로_도메인_객체로_변환한다() {
            OrderJpaEntity order = OrderJpaEntity.builder()
                    .id(1L)
                    .orderRewards(new ArrayList<>())
                    .build();
            RewardJpaEntity reward = new RewardJpaEntity(
                    1L, null, "리워드A", "설명", 100, 50,
                    null, 10000L, LocalDateTime.now(), 3000L
            );

            OrderRewardJpaEntity entity = new OrderRewardJpaEntity(1L, order, reward, 2, 20000L, 3000L);

            OrderReward orderReward = orderRewardMapper.toDomain(entity);

            assertThat(orderReward.getId()).isEqualTo(1L);
            assertThat(orderReward.getQty()).isEqualTo(2);
            assertThat(orderReward.getAmount()).isEqualTo(20000L);
            assertThat(orderReward.getShippingCharge()).isEqualTo(3000L);
        }
    }
}
