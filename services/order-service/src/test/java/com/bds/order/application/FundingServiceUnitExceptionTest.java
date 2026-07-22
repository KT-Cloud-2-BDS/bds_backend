package com.bds.order.application;

import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.reward.RewardRepository;
import com.bds.order.global.exception.BusinessException;
import com.bds.order.global.exception.ErrorCode;
import com.bds.order.infrastructure.scheduler.FundingTaskScheduler;
import com.bds.order.presentation.dto.FundingCreateRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class FundingServiceUnitExceptionTest {

    @Mock
    private FundingRepository fundingRepository;
    @Mock
    private RewardRepository rewardRepository;
    @Mock
    private FundingTaskScheduler fundingTaskScheduler;
    @InjectMocks
    private FundingService fundingService;

    @Nested
    @DisplayName("펀딩 목록 조회 예외")
    class GetFundingsExceptionTest {

        @ParameterizedTest(name = "유효하지 않은 status: {0}")
        @ValueSource(strings = {"INVALID", "WRONG", "active_typo", "123"})
        void 유효하지_않은_status면_예외를_던진다(String invalidStatus) {
            assertThatThrownBy(() -> fundingService.getFundings(invalidStatus))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_INPUT);
        }
    }

    @Nested
    @DisplayName("펀딩 상세 조회 예외")
    class GetFundingDetailExceptionTest {

        @Test
        void 존재하지_않는_펀딩이면_FUNDING_NOT_FOUND를_던진다() {
            given(fundingRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> fundingService.getFundingDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.FUNDING_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("펀딩 생성 예외")
    class CreateFundingExceptionTest {
        LocalDateTime now = LocalDateTime.now();

        @Test
        void MAKER가_아니면_ACCESS_DENIED를_던진다() {

            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.plusDays(1), now.plusDays(30), now.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L))
                    , null
            );

            assertThatThrownBy(() -> fundingService.createFunding(100L, "", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ACCESS_DENIED);
        }

        @Test
        void startAt이_현재보다_과거이면_INVALID_START_DATE를_던진다() {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.minusDays(1), now.plusDays(30), now.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L))
                    , null
            );

            assertThatThrownBy(() -> fundingService.createFunding(100L, "MAKER", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_START_DATE);
        }

        @Test
        void startAt이_holdTo보다_같거나_이후이면_INVALID_DATE_RANGE를_던진다() {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.plusDays(30), now.plusDays(30), now.plusDays(31),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L))
                    , null
            );

            assertThatThrownBy(() -> fundingService.createFunding(100L, "MAKER", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        void startAt이_holdTo보다_이후이면_INVALID_DATE_RANGE를_던진다() {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.plusDays(31), now.plusDays(30), now.plusDays(32),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L))
                    , null
            );

            assertThatThrownBy(() -> fundingService.createFunding(100L, "MAKER", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_DATE_RANGE);
        }

        @Test
        void payAt이_holdTo보다_이전이면_INVALID_PAY_DATE를_던진다() {
            FundingCreateRequestDto request = new FundingCreateRequestDto(
                    "새 펀딩", 1000000L, now.plusDays(1), now.plusDays(30), now.plusDays(29),
                    List.of(new FundingCreateRequestDto.RewardCreateDto(
                            "리워드A", "설명", 100, null, 10000L, now.plusDays(60), 3000L))
                    , null
            );

            assertThatThrownBy(() -> fundingService.createFunding(100L, "MAKER", request))
                    .isInstanceOf(BusinessException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.INVALID_PAY_DATE);
        }
    }
}
