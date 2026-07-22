package com.bds.order.infrastructure.scheduler;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.TaskScheduler;

import java.time.Instant;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FundingTaskSchedulerUnitExceptionTest {

    @InjectMocks
    private FundingTaskScheduler fundingTaskScheduler;

    @Mock
    private TaskScheduler taskScheduler;

    @Mock
    private FundingStatusUpdater fundingStatusUpdater;

    @Test
    @DisplayName("scheduleHoldToJudgment에서 TaskRejectedException 발생 시 예외를 삼키고 로그를 남긴다")
    void scheduleHoldToJudgment_taskRejected_logsError() {

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenThrow(new TaskRejectedException("rejected"));

        fundingTaskScheduler.scheduleHoldToJudgment(1L, LocalDateTime.now().plusDays(1));

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }

    @Test
    @DisplayName("scheduleActivation에서 TaskRejectedException 발생 시 예외를 삼키고 로그를 남긴다")
    void scheduleActivation_taskRejected_logsError() {

        when(taskScheduler.schedule(any(Runnable.class), any(Instant.class)))
                .thenThrow(new TaskRejectedException("rejected"));

        fundingTaskScheduler.scheduleActivation(1L, LocalDateTime.now().plusDays(1));

        verify(taskScheduler).schedule(any(Runnable.class), any(Instant.class));
    }
}
