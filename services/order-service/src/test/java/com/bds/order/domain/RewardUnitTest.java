package com.bds.order.domain;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        void 수량이_1일_때_가격을_그대로_반환한다() {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.EARLY_BIRD, 25000L, LocalDateTime.now(), 3000L);

            assertThat(reward.calculateAmount(1)).isEqualTo(25000L);
        }
    }

    @Nested
    @DisplayName("재고 충분 여부 확인")
    class IsStockSufficientTest {

        @ParameterizedTest(name = "remainQty={0}, requiredQty={1} → {2}")
        @CsvSource({
                "10, 5, true",
                "10, 10, true",
                "10, 11, false",
                "0, 1, false",
                "1, 1, true",
        })
        void 재고_충분_여부를_반환한다(int remainQty, int requiredQty, boolean expected) {
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, remainQty,
                    null, 10000L, LocalDateTime.now(), 3000L);

            assertThat(reward.isStockSufficient(requiredQty)).isEqualTo(expected);
        }
    }
}
