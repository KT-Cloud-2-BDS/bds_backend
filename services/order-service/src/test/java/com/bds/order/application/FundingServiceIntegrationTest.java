package com.bds.order.application;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaEntity;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import com.bds.order.presentation.dto.FundingCreateResponseDto;
import com.bds.order.presentation.dto.FundingDetailResponseDto;
import com.bds.order.presentation.dto.FundingListResponseDto;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FundingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FundingService fundingService;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @Autowired
    private RewardJpaRepository rewardJpaRepository;

    private FundingJpaEntity activeFunding;
    private FundingJpaEntity scheduledFunding;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        activeFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "활성 펀딩", 100L, FundingStatus.ACTIVE,
                now.minusDays(10), now.plusDays(30), now.plusDays(60),
                5, 1000000L, 500000L, false, new ArrayList<>()
        ));

        rewardJpaRepository.save(new RewardJpaEntity(
                null, activeFunding, "리워드A", "설명A", 100, 50,
                null, 10000L, now.plusDays(60), 3000L
        ));

        rewardJpaRepository.save(new RewardJpaEntity(
                null, activeFunding, "리워드B", "설명B", 50, 30,
                null, 20000L, now.plusDays(60), 5000L
        ));

        scheduledFunding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "예정 펀딩", 101L, FundingStatus.SCHEDULED,
                now.plusDays(1), now.plusDays(30), now.plusDays(60),
                0, 2000000L, 0L, false, new ArrayList<>()
        ));
    }

    @AfterEach
    void tearDown() {
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }

    @Nested
    @DisplayName("펀딩 목록 조회 통합테스트")
    class GetFundingsIntegrationTest {

        // status 없이 전체 조회 시 모든 펀딩을 반환한다
        @Test
        void 전체_펀딩_목록을_반환한다() {
            List<FundingListResponseDto> result = fundingService.getFundings(null);

            assertThat(result).hasSize(2);
        }

        // ACTIVE status 필터 시 해당 상태만 반환한다
        @Test
        void ACTIVE_상태만_필터링하여_반환한다() {
            List<FundingListResponseDto> result = fundingService.getFundings("ACTIVE");

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("활성 펀딩");
            assertThat(result.get(0).status()).isEqualTo("ACTIVE");
        }
    }

    @Nested
    @DisplayName("펀딩 상세 조회 통합테스트")
    class GetFundingDetailIntegrationTest {

        // 펀딩 상세 조회 시 리워드 목록도 함께 반환한다
        @Test
        void 펀딩_상세와_리워드를_함께_반환한다() {
            FundingDetailResponseDto result = fundingService.getFundingDetail(activeFunding.getId());

            assertThat(result.id()).isEqualTo(activeFunding.getId());
            assertThat(result.title()).isEqualTo("활성 펀딩");
            assertThat(result.status()).isEqualTo("ACTIVE");
            assertThat(result.rewards()).hasSize(2);
            assertThat(result.rewards().get(0).name()).isEqualTo("리워드A");
        }

    }

    @Nested
    @DisplayName("펀딩 생성 통합테스트")
    class CreateFundingIntegrationTest {

        // 펀딩 + 리워드를 동시에 생성하고 DB에 저장한다
        @Test
        void 펀딩과_리워드를_동시에_생성한다() {
            LocalDateTime now = LocalDateTime.now();
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 3000000L, now.plusDays(1), now.plusDays(30), now.plusDays(31),
                    List.of(
                            new FundingCreateRequestDto.RewardCreateDto(
                                    "리워드X", "설명X", 200, "EARLY_BIRD", 15000L, now.plusDays(60), 4000L),
                            new FundingCreateRequestDto.RewardCreateDto(
                                    "리워드Y", "설명Y", 100, null, 30000L, now.plusDays(60), 5000L)
                    )
            );

            FundingCreateResponseDto result = fundingService.createFunding(200L, "MAKER", request);

            assertThat(result.fundingId()).isNotNull();
            assertThat(result.title()).isEqualTo("새 펀딩");
            assertThat(result.status()).isEqualTo("SCHEDULED");

            FundingDetailResponseDto detail = fundingService.getFundingDetail(result.fundingId());
            assertThat(detail.rewards()).hasSize(2);
            assertThat(detail.rewards().get(0).name()).isEqualTo("리워드X");
            assertThat(detail.rewards().get(0).badgeType()).isEqualTo("EARLY_BIRD");
            assertThat(detail.rewards().get(0).limitQty()).isEqualTo(200);
            assertThat(detail.rewards().get(0).remainQty()).isEqualTo(200);
        }
    }
}
