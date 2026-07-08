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
class RewardPersistenceAdaptorUnitTest {

    @Mock
    private RewardJpaRepository rewardJpaRepository;

    @Mock
    private RewardMapper rewardMapper;

    @InjectMocks
    private RewardPersistenceAdaptor rewardPersistenceAdaptor;

    @Nested
    @DisplayName("펀딩ID와 리워드ID 목록으로 조회")
    class FindAllByIdAndFundingIdTest {

        @Test
        void 리워드_목록을_반환한다() {
            List<Long> ids = List.of(1L, 2L);
            Long fundingId = 1L;
            LocalDateTime now = LocalDateTime.now();

            RewardJpaEntity entity = new RewardJpaEntity(
                    1L, null, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L
            );
            Reward reward = Reward.of(1L, fundingId, "리워드A", "설명", 100, 50,
                    BadgeType.ULTRA_EARLY_BIRD, 10000L, now.plusDays(60), 3000L);

            given(rewardJpaRepository.findAllByIdAndFundingId(ids, fundingId))
                    .willReturn(List.of(entity));
            given(rewardMapper.toDomain(entity)).willReturn(reward);

            List<Reward> result = rewardPersistenceAdaptor.findAllByIdAndFundingId(ids, fundingId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("리워드A");
        }
    }

    @Nested
    @DisplayName("리워드 재고 복원")
    class IncreaseRemainQtyTest {

        @Test
        void 재고_증가_메서드를_호출한다() {
            rewardPersistenceAdaptor.increaseRemainQty(1L, 3);

            verify(rewardJpaRepository).increaseRemainQty(1L, 3);
        }
    }
}
