package com.bds.order.fixture;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.funding.FundingType;
import com.bds.order.infrastructure.funding.FundingJpaEntity;

import java.time.LocalDateTime;

public class FundingFixture {

    private static final LocalDateTime NOW = LocalDateTime.now();

    public static Funding createFunding(Long id, FundingStatus status, Long currentAmount, Long goalAmount, LocalDateTime startAt, LocalDateTime holdTo) {
        return Funding.of(
                id, "테스트 펀딩", 100L, status, FundingType.INSTANT,
                startAt, holdTo, holdTo.plusDays(1),
                0, goalAmount, currentAmount, null,
                NOW, NOW
        );
    }

    public static Funding createReservedFunding(Long id, FundingStatus status, Long currentAmount, Long goalAmount, LocalDateTime startAt, LocalDateTime holdTo) {
        return Funding.of(
                id, "테스트 펀딩", 100L, status, FundingType.RESERVED,
                startAt, holdTo, holdTo.plusDays(1),
                0, goalAmount, currentAmount, null,
                NOW, NOW
        );
    }

    public static Funding createActiveFunding(Long id, Long currentAmount, Long goalAmount) {
        return createFunding(id, FundingStatus.ACTIVE, currentAmount, goalAmount,
                NOW.minusDays(10), NOW.plusDays(30));
    }

    public static Funding createScheduledFunding(Long id, LocalDateTime startAt, LocalDateTime holdTo) {
        return createFunding(id, FundingStatus.SCHEDULED, 0L, 1000000L, startAt, holdTo);
    }

    public static Funding createSuccessFunding(Long id) {
        return createFunding(id, FundingStatus.SUCCESS, 1000000L, 500000L,
                NOW.minusDays(30), NOW.minusDays(1));
    }

    public static Funding createFailedFunding(Long id) {
        return createFunding(id, FundingStatus.FAILED, 100000L, 500000L,
                NOW.minusDays(30), NOW.minusDays(1));
    }

    public static FundingJpaEntity createFundingJpaEntity() {
        return createFundingJpaEntity(FundingType.INSTANT);
    }

    public static FundingJpaEntity createFundingJpaEntity(FundingType type) {
        return FundingJpaEntity.builder()
                .title("테스트 펀딩")
                .creatorId(100L)
                .status(FundingStatus.ACTIVE)
                .type(type)
                .startAt(NOW.minusDays(10))
                .holdTo(NOW.plusDays(30))
                .payAt(NOW.plusDays(31))
                .participationCnt(0)
                .goalAmount(1000000L)
                .currentAmount(0L)
                .isSuccess(null)
                .build();
    }

    public static FundingJpaEntity createFundingJpaEntity(FundingStatus stats) {
        return FundingJpaEntity.builder()
                .title("테스트 펀딩")
                .creatorId(100L)
                .status(stats)
                .type(FundingType.INSTANT)
                .startAt(NOW.minusDays(10))
                .holdTo(NOW.plusDays(30))
                .payAt(NOW.plusDays(31))
                .participationCnt(0)
                .goalAmount(1000000L)
                .currentAmount(0L)
                .isSuccess(null)
                .build();
    }
}
