package com.bds.order.domain;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class RewardUnitTest {

    @Nested
    @DisplayName("리워드 총 금액 계산")
    class CalculateAmountTest {

        @Test
        void 가격과_수량을_곱한_금액을_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.calculateAmount(3)).isEqualTo(30000L);
        }

        @Test
        void 수량이_1이면_단가를_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    null, 25000L, LocalDateTime.now(), 3000L);

            assertThat(reward.calculateAmount(1)).isEqualTo(25000L);
        }
    }

    @Nested
    @DisplayName("리워드 재고 검증")
    class IsStockSufficientTest {

        @Test
        void 재고가_충분하면_true를_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 10,
                    null, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.isStockSufficient(5)).isTrue();
        }

        @Test
        void 재고와_요청수량이_같으면_true를_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 5,
                    null, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.isStockSufficient(5)).isTrue();
        }

        @Test
        void 재고가_부족하면_false를_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 3,
                    null, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.isStockSufficient(5)).isFalse();
        }

        @Test
        void 재고가_0이면_false를_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 0,
                    null, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.isStockSufficient(1)).isFalse();
        }
    }
}