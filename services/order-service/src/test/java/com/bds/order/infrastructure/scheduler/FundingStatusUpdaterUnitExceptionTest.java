package com.bds.order.infrastructure.scheduler;

import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.fixture.FundingFixture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingStatusUpdaterUnitExceptionTest {

    @InjectMocks
    private FundingStatusUpdater fundingStatusUpdater;

    @Mock
    private FundingRepository fundingRepository;

    @Test
    void 존재하지_않는_fundingId로_judgeFunding_호출_시_IllegalStateException_발생() {
        when(fundingRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fundingStatusUpdater.judgeFunding(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_JUDGE] Funding not found: fundingId=999");
    }

    @ParameterizedTest(name = "{0} 상태에서는 판정 불가")
    @EnumSource(value = FundingStatus.class, names = {"SCHEDULED", "SUCCESS", "FAILED"})
    void 판정_가능_상태가_아닌_펀딩은_IllegalStateException_발생(FundingStatus status) {
        Funding funding = FundingFixture.createFunding(1L, status, 1000000L, 500000L,
                LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

        when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

        assertThatThrownBy(() -> fundingStatusUpdater.judgeFunding(1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_JUDGE] 판정 불가 상태");
    }

    @Test
    void 존재하지_않는_fundingId로_activateFunding_호출_시_IllegalStateException_발생() {
        when(fundingRepository.findByIdForUpdate(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fundingStatusUpdater.activateFunding(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("[FUNDING_ACTIVATE] Funding not found: fundingId=999");
    }

    @ParameterizedTest(name = "{0} 상태에서는 활성화 불가")
    @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING", "SUCCESS", "FAILED"})
    void SCHEDULED가_아닌_펀딩은_활성화하지_않는다(FundingStatus status) {
        Funding funding = FundingFixture.createFunding(1L, status, 0L, 1000000L,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().plusDays(30));

        when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

        fundingStatusUpdater.activateFunding(1L);

        verify(fundingRepository, never()).save(any());
    }
}
