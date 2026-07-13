package com.bds.order.domain;

import com.bds.order.domain.funding.Funding;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class FundingUnitTest {

    @Test
    void 현재가_펀딩시작_이전이면_false_반환() {
        LocalDateTime now = LocalDateTime.now();
        Funding funding = Funding.of(null, null, null, null, now.plusDays(1), null, null, 0, null, null, null, null, null);
        assertThat(funding.isFuningPeriod(now)).isFalse();
    }

    @Test
    void 현재가_펀딩종료_이후라면_false_반환() {
        LocalDateTime now = LocalDateTime.now();
        Funding funding = Funding.of(null, null, null, null, now.minusDays(2), now.minusDays(1), null, 0, null, null, null, null, null);
        assertThat(funding.isFuningPeriod(now)).isFalse();
    }

    @Test
    void 현재가_펀딩시작과_펀딩종료_사이라면_true_반환() {
        LocalDateTime now = LocalDateTime.now();
        Funding funding = Funding.of(null, null, null, null, now.minusDays(1), now.plusDays(1), null, 0, null, null, null, null, null);
        assertThat(funding.isFuningPeriod(now)).isTrue();
    }

}
