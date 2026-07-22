package com.bds.order.application;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.order.infrastructure.reward.RewardJpaRepository;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FundingServiceIntegrationExceptionTest extends AbstractIntegrationTest {

    @Autowired
    private FundingService fundingService;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @Autowired
    private RewardJpaRepository rewardJpaRepository;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();

        fundingJpaRepository.save(new FundingJpaEntity(
                null, "활성 펀딩", 100L, FundingStatus.ACTIVE, FundingType.INSTANT,
                now.minusDays(10), now.plusDays(30), now.plusDays(60),
                5, 1000000L, 500000L, false, new ArrayList<>()
        ));
    }

    @AfterEach
    void tearDown() {
        rewardJpaRepository.deleteAll();
        fundingJpaRepository.deleteAll();
    }


    @Nested
    @DisplayName("펀딩 상세 조회 예외")
    class GetFundingDetailExceptionTest {

        // 존재하지 않는 fundingId로 조회 시 FUNDING_NOT_FOUND를 던진다
        @Test
        void 존재하지_않는_펀딩이면_예외를_던진다() {
            assertThatThrownBy(() -> fundingService.getFundingDetail(9999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
        }
    }
}
