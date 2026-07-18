package com.bds.order.domain;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.fixture.FundingFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FundingUnitTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2025, 7, 1, 12, 0);

    @Nested
    @DisplayName("펀딩 기간 확인")
    class IsFundingPeriodTest {

        @Test
        void 현재가_펀딩시작_이전이면_false_반환() {
            Funding funding = Funding.of(null, null, null, null, NOW.plusDays(1), NOW.plusDays(30), null, 0, null, null, null, null, null);
            assertThat(funding.isFundingPeriod(NOW)).isFalse();
        }

        @Test
        void 현재가_펀딩종료_이후이면_false_반환() {
            Funding funding = Funding.of(null, null, null, null, NOW.minusDays(30), NOW.minusDays(1), null, 0, null, null, null, null, null);
            assertThat(funding.isFundingPeriod(NOW)).isFalse();
        }

        @Test
        void 현재가_펀딩기간_내이면_true_반환() {
            Funding funding = Funding.of(null, null, null, null, NOW.minusDays(10), NOW.plusDays(10), null, 0, null, null, null, null, null);
            assertThat(funding.isFundingPeriod(NOW)).isTrue();
        }
    }

    @Nested
    @DisplayName("펀딩 성공 처리")
    class MarkSuccessTest {

        @Test
        void 성공_처리_시_상태와_플래그가_변경된다() {
            Funding funding = FundingFixture.createActiveFunding(1L, 1000000L, 500000L);

            funding.markSuccess();

            assertThat(funding.getStatus()).isEqualTo(FundingStatus.SUCCESS);
            assertThat(funding.getIsSuccess()).isTrue();
        }
    }

    @Nested
    @DisplayName("펀딩 실패 처리")
    class MarkFailedTest {

        @Test
        void 실패_처리_시_상태와_플래그가_변경된다() {
            Funding funding = FundingFixture.createActiveFunding(1L, 100000L, 500000L);

            funding.markFailed();

            assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
            assertThat(funding.getIsSuccess()).isFalse();
        }
    }

    @Nested
    @DisplayName("펀딩 활성화")
    class ActivateTest {

        @Test
        void 활성화_시_상태가_ACTIVE로_변경된다() {
            Funding funding = FundingFixture.createScheduledFunding(1L, NOW.plusDays(1), NOW.plusDays(30));

            funding.activate();

            assertThat(funding.getStatus()).isEqualTo(FundingStatus.ACTIVE);
        }
    }

    @Nested
    @DisplayName("펀딩 생성")
    class CreateTest {

        @Test
        void 정적_팩토리_메서드로_생성_시_기본값이_설정된다() {
            Funding funding = Funding.create("테스트 펀딩", 100L, 1000000L,
                    NOW, NOW.plusDays(30), NOW.plusDays(31));

            assertThat(funding.getId()).isNull();
            assertThat(funding.getTitle()).isEqualTo("테스트 펀딩");
            assertThat(funding.getCreatorId()).isEqualTo(100L);
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.SCHEDULED);
            assertThat(funding.getGoalAmount()).isEqualTo(1000000L);
            assertThat(funding.getCurrentAmount()).isEqualTo(0L);
            assertThat(funding.getParticipationCnt()).isEqualTo(0);
            assertThat(funding.getIsSuccess()).isFalse();
        }
    }
}
