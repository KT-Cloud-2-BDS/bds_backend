package com.bds.order.infrastructure.scheduler;

import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.infrastructure.funding.FundingJpaEntity;
import com.bds.order.infrastructure.funding.FundingJpaRepository;
import com.bds.support.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.assertj.core.api.Assertions.assertThat;

class FundingScheduleInitializerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private FundingScheduleInitializer fundingScheduleInitializer;

    @Autowired
    private FundingJpaRepository fundingJpaRepository;

    @AfterEach
    void tearDown() {
        fundingJpaRepository.deleteAll();
    }

    @Test
    @DisplayName("startAt이 이미 지난 SCHEDULED 펀딩이 ACTIVE로 전환된다")
    void init_missedActivation_becomesActive() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.SCHEDULED,
                now.minusDays(5), now.plusDays(30), now.plusDays(31),
                0, 1000000L, 0L, null, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.ACTIVE);
    }

    @Test
    @DisplayName("holdTo가 이미 지난 ACTIVE 펀딩이 SUCCESS로 판정된다 (목표 달성)")
    void init_missedJudgment_success() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.ACTIVE,
                now.minusDays(30), now.minusDays(1), now.plusDays(1),
                0, 500000L, 1000000L, null, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.SUCCESS);
        assertThat(updated.getIsSuccess()).isTrue();
    }

    @Test
    @DisplayName("holdTo가 이미 지난 ACTIVE 펀딩이 FAILED로 판정된다 (목표 미달)")
    void init_missedJudgment_failure() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.ACTIVE,
                now.minusDays(30), now.minusDays(1), now.plusDays(1),
                0, 1000000L, 100000L, null, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.FAILED);
        assertThat(updated.getIsSuccess()).isFalse();
    }

    @Test
    @DisplayName("startAt이 미래인 SCHEDULED 펀딩은 상태가 변경되지 않는다")
    void init_futureScheduled_remainsScheduled() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.SCHEDULED,
                now.plusDays(5), now.plusDays(30), now.plusDays(31),
                0, 1000000L, 0L, null, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.SCHEDULED);
    }

    @Test
    @DisplayName("holdTo가 미래인 ACTIVE 펀딩은 상태가 변경되지 않는다")
    void init_activeWithFutureHoldTo_remainsActive() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.ACTIVE,
                now.minusDays(10), now.plusDays(30), now.plusDays(31),
                0, 1000000L, 500000L, null, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 판정 완료된 SUCCESS 펀딩은 변경되지 않는다")
    void init_alreadySuccess_remainsSuccess() {

        LocalDateTime now = LocalDateTime.now();
        FundingJpaEntity funding = fundingJpaRepository.save(new FundingJpaEntity(
                null, "테스트 펀딩", 100L, FundingStatus.SUCCESS,
                now.minusDays(30), now.minusDays(1), now.plusDays(1),
                0, 500000L, 1000000L, true, new ArrayList<>()
        ));


        fundingScheduleInitializer.init();


        FundingJpaEntity updated = fundingJpaRepository.findById(funding.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(FundingStatus.SUCCESS);
    }
}
