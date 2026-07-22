package com.bds.order.infrastructure.scheduler;

import com.bds.order.infrastructure.funding.FundingJudgmentOrchestrator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FundingTaskSchedulerUnitTest {

    @InjectMocks
    private FundingTaskScheduler fundingTaskScheduler;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private FundingStatusUpdater fundingStatusUpdater;

    @Mock
    private FundingJudgmentOrchestrator fundingJudgmentOrchestrator;

    @Test
    @DisplayName("scheduleHoldToJudgment 호출 시 TaskScheduler에 정확한 시간으로 예약된다")
    void scheduleHoldToJudgment_schedulesAtCorrectTime() {

        Long fundingId = 1L;
        LocalDateTime holdTo = LocalDateTime.of(2025, 8, 1, 12, 0);
        Instant expectedInstant = holdTo.atZone(ZoneId.systemDefault()).toInstant();


        fundingTaskScheduler.scheduleHoldToJudgment(fundingId, holdTo);


        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("scheduleHoldToJudgment 예약된 작업이 실행되면 orchestrator.execute를 호출한다")
    void scheduleHoldToJudgment_executesOrchestrator() {
        Long fundingId = 1L;
        LocalDateTime holdTo = LocalDateTime.of(2025, 8, 1, 12, 0);

        fundingTaskScheduler.scheduleHoldToJudgment(fundingId, holdTo);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

        runnableCaptor.getValue().run();

        verify(fundingJudgmentOrchestrator).execute(fundingId);
    }


    @Test
    @DisplayName("scheduleActivation 호출 시 TaskScheduler에 정확한 시간으로 예약된다")
    void scheduleActivation_schedulesAtCorrectTime() {

        Long fundingId = 1L;
        LocalDateTime startAt = LocalDateTime.of(2025, 7, 15, 9, 0);
        Instant expectedInstant = startAt.atZone(ZoneId.systemDefault()).toInstant();


        fundingTaskScheduler.scheduleActivation(fundingId, startAt);


        ArgumentCaptor<Instant> instantCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(taskScheduler).schedule(any(Runnable.class), instantCaptor.capture());
        assertThat(instantCaptor.getValue()).isEqualTo(expectedInstant);
    }

    @Test
    @DisplayName("scheduleActivation 예약된 작업이 실행되면 activateFunding을 호출한다")
    void scheduleActivation_executesActivateFunding() {
        Long fundingId = 1L;
        LocalDateTime startAt = LocalDateTime.of(2025, 7, 15, 9, 0);

        fundingTaskScheduler.scheduleActivation(fundingId, startAt);

        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(taskScheduler).schedule(runnableCaptor.capture(), any(Instant.class));

        runnableCaptor.getValue().run();

        verify(fundingStatusUpdater).activateFunding(fundingId);
    }
}
