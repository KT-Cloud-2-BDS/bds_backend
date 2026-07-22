package com.bds.order.infrastructure.reward;

import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class RewardPersistenceAdapterUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.now();
    @Mock
    private RewardJpaRepository rewardJpaRepository;
    @Mock
    private RewardMapper rewardMapper;
    @InjectMocks
    private RewardPersistenceAdapter rewardPersistenceAdapter;

    @Nested
    @DisplayName("펀딩ID와 리워드ID 목록으로 조회")
    class FindAllByIdAndFundingIdTest {

        @Test
        void 리워드_목록을_반환한다() {
            List<Long> ids = List.of(1L, 2L);
            Long fundingId = 1L;

            RewardJpaEntity entity = new RewardJpaEntity(
                    1L, null, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, NOW.plusDays(60), 3000L
            );
            Reward reward = Reward.of(1L, fundingId, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, NOW.plusDays(60), 3000L);

            given(rewardJpaRepository.findAllByIdAndFundingId(ids, fundingId))
                    .willReturn(List.of(entity));
            given(rewardMapper.toDomain(entity)).willReturn(reward);

            List<Reward> result = rewardPersistenceAdapter.findAllByIdAndFundingId(ids, fundingId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("리워드A");
        }
    }

    @Nested
    @DisplayName("펀딩ID로 리워드 조회")
    class FindByFundingIdTest {

        @Test
        void 해당_펀딩의_리워드_목록을_반환한다() {
            RewardJpaEntity entity = new RewardJpaEntity(
                    1L, null, "리워드A", "설명", 100, 50,
                    null, 10000L, NOW.plusDays(60), 3000L
            );
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    null, 10000L, NOW.plusDays(60), 3000L);

            given(rewardJpaRepository.findByFundingId(1L)).willReturn(List.of(entity));
            given(rewardMapper.toDomain(entity)).willReturn(reward);

            List<Reward> result = rewardPersistenceAdapter.findByFundingId(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getFundingId()).isEqualTo(1L);
        }

        @Test
        void 리워드가_없으면_빈_목록을_반환한다() {
            given(rewardJpaRepository.findByFundingId(999L)).willReturn(List.of());

            List<Reward> result = rewardPersistenceAdapter.findByFundingId(999L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("리워드 재고 복원")
    class IncreaseRemainQtyTest {

        @Test
        void 재고_증가_메서드를_호출한다() {
            rewardPersistenceAdapter.increaseRemainQty(1L, 3);

            verify(rewardJpaRepository).increaseRemainQty(1L, 3);
        }
    }

    @Nested
    @DisplayName("재고 차감")
    class DecreaseStockTest {

        @Test
        void 재고_차감_성공_시_1을_반환한다() {
            given(rewardJpaRepository.decreaseStock(1L, 2)).willReturn(1);

            int result = rewardPersistenceAdapter.decreaseStock(1L, 2);

            assertThat(result).isEqualTo(1);
        }

        @Test
        void 재고_부족_시_0을_반환한다() {
            given(rewardJpaRepository.decreaseStock(1L, 100)).willReturn(0);

            int result = rewardPersistenceAdapter.decreaseStock(1L, 100);

            assertThat(result).isEqualTo(0);
        }
    }
}