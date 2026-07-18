package com.bds.order.infrastructure.scheduler;

import com.bds.order.application.OrderService;
import com.bds.order.domain.funding.Funding;
import com.bds.order.domain.funding.FundingRepository;
import com.bds.order.domain.funding.FundingStatus;
import com.bds.order.domain.order.OrderRepository;
import com.bds.order.domain.order.OrderStatus;
import com.bds.order.fixture.FundingFixture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FundingStatusUpdaterUnitTest {

    @InjectMocks
    private FundingStatusUpdater fundingStatusUpdater;

    @Mock
    private FundingRepository fundingRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderService orderService;

    @Nested
    @DisplayName("judgeFunding")
    class JudgeFundingTest {

        @ParameterizedTest(name = "{0} 상태에서 판정 진행")
        @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING"})
        void 판정_가능_상태에서_목표_달성시_SUCCESS로_변경하고_true를_반환한다(FundingStatus status) {
            Funding funding = FundingFixture.createFunding(1L, status, 1000000L, 500000L,
                    LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            boolean result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result).isTrue();
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.SUCCESS);
            assertThat(funding.getIsSuccess()).isTrue();
            verify(fundingRepository).save(funding);
        }

        @ParameterizedTest(name = "{0} 상태에서 판정 진행")
        @EnumSource(value = FundingStatus.class, names = {"ACTIVE", "HOLDING"})
        void 판정_가능_상태에서_목표_미달시_FAILED로_변경하고_false를_반환한다(FundingStatus status) {
            Funding funding = FundingFixture.createFunding(1L, status, 100000L, 500000L,
                    LocalDateTime.now().minusDays(30), LocalDateTime.now().minusDays(1));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            boolean result = fundingStatusUpdater.judgeFunding(1L);

            assertThat(result).isFalse();
            assertThat(funding.getStatus()).isEqualTo(FundingStatus.FAILED);
            assertThat(funding.getIsSuccess()).isFalse();
            verify(fundingRepository).save(funding);
        }
    }

    @Nested
    @DisplayName("handleFundingSuccess")
    class HandleFundingSuccessTest {

        @Test
        void PAID_주문에_정산_요청하고_RESERVED_주문에_결제_요청한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(2L));

            fundingStatusUpdater.handleFundingSuccess(1L);

            verify(orderService).publishSettlement(1L);
            verify(orderService).processPayingAndPublishSettlement(2L);
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of());
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleFundingSuccess(1L);

            verifyNoInteractions(orderService);
        }
    }

    @Nested
    @DisplayName("handleFundingFailure")
    class HandleFundingFailureTest {

        @Test
        void PAID_주문에_환불_요청하고_RESERVED_주문을_취소한다() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of(1L));
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of(2L));

            fundingStatusUpdater.handleFundingFailure(1L);

            verify(orderService).processCancelAndPublishRefund(1L);
            verify(orderService).processCancelledUpdate(2L, "FUNDING_FAILED");
        }

        @Test
        void 해당_펀딩에_주문이_없으면_orderService_호출_없음() {
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.PAID), eq(0L), eq(500)))
                    .thenReturn(List.of());
            when(orderRepository.findOrderIdsByFundingIdAndStatus(eq(1L), eq(OrderStatus.RESERVED), eq(0L), eq(500)))
                    .thenReturn(List.of());

            fundingStatusUpdater.handleFundingFailure(1L);

            verifyNoInteractions(orderService);
        }
    }

    @Nested
    @DisplayName("activateFunding")
    class ActivateFundingTest {

        @Test
        void SCHEDULED_펀딩을_ACTIVE로_전환한다() {
            Funding funding = FundingFixture.createScheduledFunding(1L,
                    LocalDateTime.now().minusDays(1),
                    LocalDateTime.now().plusDays(30));

            when(fundingRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(funding));

            fundingStatusUpdater.activateFunding(1L);

            assertThat(funding.getStatus()).isEqualTo(FundingStatus.ACTIVE);
            verify(fundingRepository).save(funding);
        }
    }
}
