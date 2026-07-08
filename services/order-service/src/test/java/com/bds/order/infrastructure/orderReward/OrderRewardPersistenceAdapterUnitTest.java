package com.bds.order.infrastructure.orderReward;

import com.bds.order.domain.reward.BadgeType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OrderRewardPersistenceAdapterUnitTest {

    @Mock
    private OrderRewardJpaRepository orderRewardJpaRepository;

    @InjectMocks
    private OrderRewardPersistenceAdapter orderRewardPersistenceAdapter;

    @Nested
    @DisplayName("주문 리워드 상세 목록 조회")
    class FindOrderRewardDetailsTest {

        @Test
        void 주문ID로_리워드_상세_목록을_반환한다() {
            Long orderId = 1L;
            OrderRewardDetailProjection projection = new OrderRewardDetailProjection(
                    1L, 2, 20000L, 3000L, "리워드A", BadgeType.ULTRA_EARLY_BIRD
            );

            given(orderRewardJpaRepository.findOrderRewardDetailsWithReward(orderId))
                    .willReturn(List.of(projection));

            List<OrderRewardDetailProjection> result = orderRewardPersistenceAdapter.findOrderRewardDetailsWithReward(orderId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).rewardName()).isEqualTo("리워드A");
            assertThat(result.get(0).qty()).isEqualTo(2);
            assertThat(result.get(0).amount()).isEqualTo(20000L);
        }

        @Test
        void 주문에_리워드가_없으면_빈_목록을_반환한다() {
            given(orderRewardJpaRepository.findOrderRewardDetailsWithReward(999L))
                    .willReturn(List.of());

            List<OrderRewardDetailProjection> result = orderRewardPersistenceAdapter.findOrderRewardDetailsWithReward(999L);

            assertThat(result).isEmpty();
        }
    }
}