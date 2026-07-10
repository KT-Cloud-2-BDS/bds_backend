package com.bds.order.infrastructure.funding;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class FundingMapperUnitTest {

    private FundingMapper fundingMapper = new FundingMapper();

    @Nested
    @DisplayName("JpaEntity에서 도메인으로 변환")
    class ToDomainTest {

        @Test
        void 정상적으로_도메인_객체로_변환한다() {
            LocalDateTime now = LocalDateTime.now();
            FundingJpaEntity entity = new FundingJpaEntity(
                    1L, "Title", 100L, FundingStatus.ACTIVE,
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
}