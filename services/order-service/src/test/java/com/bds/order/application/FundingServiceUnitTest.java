package com.bds.order.application;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.domain.reward.BadgeType;
import com.bds.order.domain.reward.Reward;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.fixture.FundingFixture;
import com.bds.order.infrastructure.scheduler.FundingTaskScheduler;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.order.presentation.dto.FundingCreateResponseDto;
import com.bds.order.presentation.dto.FundingDetailResponseDto;
import com.bds.order.presentation.dto.FundingListResponseDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitTest {

    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private RewardRepository rewardRepository;
    @Mock
    private FundingTaskScheduler fundingTaskScheduler;
    @InjectMocks
    private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 목록 조회")
    class GetFundingsTest {
        LocalDateTime now = LocalDateTime.now();

        @Test
        void status_없으면_전체_목록을_반환한다() {
            Funding funding1 = FundingFixture.createActiveFunding(1L, 500000L, 1000000L);
            Funding funding2 = FundingFixture.createScheduledFunding(2L, now.plusDays(1), now.plusDays(30));

            Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.ASC, "startAt"));
            Page<Funding> fundingPage = new PageImpl<>(List.of(funding1, funding2), pageable, 2);

            given(fundingRepository.findByTypeAndStatusIn(
                    FundingType.INSTANT,
                    List.of(FundingStatus.SCHEDULED, FundingStatus.ACTIVE, FundingStatus.SUCCESS, FundingStatus.FAILED),
                    pageable
            )).willReturn(fundingPage);

            List<FundingListResponseDto> result = fundingService.getFundings("INSTANT", "ALL", 0, 9).getContent();

            assertThat(result).hasSize(2);
        }

        @Test
        void status_지정하면_해당_상태만_반환한다() {
            Funding funding = FundingFixture.createActiveFunding(1L, 500000L, 1000000L);

            Pageable pageable = PageRequest.of(0, 9, Sort.by(Sort.Direction.ASC, "holdTo"));
            Page<Funding> fundingPage = new PageImpl<>(List.of(funding), pageable, 1);

            given(fundingRepository.findByTypeAndStatusIn(
                    FundingType.INSTANT,
                    List.of(FundingStatus.ACTIVE),
                    pageable
            )).willReturn(fundingPage);

            List<FundingListResponseDto> result = fundingService.getFundings("INSTANT", "ACTIVE", 0, 9).getContent();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).status()).isEqualTo("ACTIVE");
        }


    }

    @Nested
    @DisplayName("펀딩 상세 조회")
    class GetFundingDetailTest {
        LocalDateTime now = LocalDateTime.now();

        @Test
        void 펀딩과_리워드를_함께_반환한다() {
            Funding funding = FundingFixture.createActiveFunding(1L, 500000L, 1000000L);
            Reward reward = Reward.of(1L, 1L, "리워드A", "설명", 100, 50,
                    BadgeType.EARLY_BIRD, 10000L, now.plusDays(60), 3000L);

            given(fundingRepository.findById(1L)).willReturn(Optional.of(funding));
            given(rewardRepository.findByFundingId(1L)).willReturn(List.of(reward));

            FundingDetailResponseDto result = fundingService.getFundingDetail(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.rewards()).hasSize(1);
            assertThat(result.rewards().get(0).name()).isEqualTo("리워드A");
            assertThat(result.rewards().get(0).badgeType()).isEqualTo("EARLY_BIRD");
        }

    }

    @Nested
    @DisplayName("펀딩 생성")
    class CreateFundingTest {

        LocalDateTime now = LocalDateTime.now();

        @Test
        void 펀딩을_생성하고_스케줄러에_등록한다() {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.plusDays(1), now.plusDays(30), now.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L)),
                    null
            );

            Funding savedFunding = Funding.of(1L, "새 펀딩", 100L, FundingStatus.SCHEDULED, FundingType.INSTANT,
                    now, now.plusDays(30), now.plusDays(31),
                    0, 1000000L, 0L, false, now, now);

            given(fundingRepository.saveWithRewards(any(Funding.class), eq(request.rewards())))
                    .willReturn(savedFunding);

            FundingCreateResponseDto result = fundingService.createFunding(100L, true, request);

            assertThat(result.fundingId()).isEqualTo(1L);
            assertThat(result.title()).isEqualTo("새 펀딩");
            assertThat(result.status()).isEqualTo("SCHEDULED");
            verify(fundingTaskScheduler).scheduleHoldToJudgment(1L, now.plusDays(30));
        }
    }
}
