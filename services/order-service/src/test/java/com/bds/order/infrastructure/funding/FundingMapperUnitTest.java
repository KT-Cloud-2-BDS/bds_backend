package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class FundingMapperUnitTest {

    private final FundingMapper fundingMapper = new FundingMapper();

    @Nested
    @DisplayName("JpaEntity에서 도메인으로 변환")
    class ToDomainTest {

        @Test
        void 정상적으로_도메인_객체로_변환한다() {
            LocalDateTime now = LocalDateTime.now();
            FundingJpaEntity entity = new FundingJpaEntity(
                    1L, "Title", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    5, 1000000L, 500000L, false, new ArrayList<>()
            );

            Funding funding = fundingMapper.toDomain(entity);

            assertThat(funding.getId()).isEqualTo(1L);
            assertThat(funding.getTitle()).isEqualTo("Title");
            assertThat(funding.getCreatorId()).isEqualTo(100L);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.ACTIVE);
            assertThat(funding.getParticipationCnt()).isEqualTo(5);
            assertThat(funding.getGoalAmount()).isEqualTo(1000000L);
            assertThat(funding.getCurrentAmount()).isEqualTo(500000L);
            assertThat(funding.getIsSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("도메인에서 JpaEntity로 변환")
    class ToJpaEntityTest {

        @Test
        void 정상적으로_JpaEntity로_변환한다() {
            LocalDateTime now = LocalDateTime.now();
            Funding funding = Funding.of(1L, "Title", 100L, FundingStatus.ACTIVE, null,
                    now.minusDays(10), now.plusDays(30), now.plusDays(60),
                    5, 1000000L, 500000L, false, now, now);

            FundingJpaEntity entity = fundingMapper.toJpaEntity(funding);

            assertThat(entity.getId()).isEqualTo(1L);
            assertThat(entity.getTitle()).isEqualTo("Title");
            assertThat(entity.getCreatorId()).isEqualTo(100L);
            assertThat(entity.getStatus()).isEqualTo(FundingStatus.ACTIVE);
            assertThat(entity.getParticipationCnt()).isEqualTo(5);
            assertThat(entity.getGoalAmount()).isEqualTo(1000000L);
            assertThat(entity.getCurrentAmount()).isEqualTo(500000L);
            assertThat(entity.getIsSuccess()).isFalse();
        }

        @Test
        void id가_null이면_JpaEntity_id도_null이다() {
            Funding funding = Funding.create("Title", 100L, 1000000L,
                    LocalDateTime.now(), LocalDateTime.now().plusDays(30), LocalDateTime.now().plusDays(31), null);

            FundingJpaEntity entity = fundingMapper.toJpaEntity(funding);

            assertThat(entity.getId()).isNull();
            assertThat(entity.getStatus()).isEqualTo(FundingStatus.SCHEDULED);
        }
    }
}
